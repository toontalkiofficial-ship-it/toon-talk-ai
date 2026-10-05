import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const models: Record<string, { kind: "image" | "video"; providerModel: string }> = {
  "black-forest-labs/flux.1-schnell": { kind: "image", providerModel: "black-forest-labs/flux.1-schnell" },
  "flux": { kind: "image", providerModel: "black-forest-labs/flux.1-schnell" },
  "black-forest-labs/flux.2-pro": { kind: "image", providerModel: "black-forest-labs/flux.2-pro" },
  "openai/gpt-image-1.5": { kind: "image", providerModel: "openai/gpt-image-1.5" },
  "bytedance/seedance-1-pro-fast": { kind: "video", providerModel: "bytedance/seedance-1-pro-fast" },
  "google/veo-3.1-fast": { kind: "video", providerModel: "google/veo-3.1-fast" },
  "bytedance/seedance-2.0": { kind: "video", providerModel: "bytedance/seedance-2.0" },
};

function json(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { ...corsHeaders, "Cache-Control": "no-store" } });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const providerKey = Deno.env.get("POLLINATIONS_API_KEY");
  const costJson = Deno.env.get("MODEL_CREDIT_COSTS_JSON");
  if (!url || !anonKey || !serviceKey || !providerKey || !costJson) {
    return json({ error: "generation_backend_not_configured" }, 503);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await userClient.auth.getUser();
  if (authError || !authData.user) return json({ error: "unauthorized" }, 401);

  let input: { kind?: string; model?: string; prompt?: string; idempotencyKey?: string };
  try { input = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  const model = input.model ? models[input.model] : undefined;
  const prompt = typeof input.prompt === "string" ? input.prompt.trim() : "";
  const idempotencyKey = typeof input.idempotencyKey === "string" ? input.idempotencyKey : "";
  if (!model || model.kind !== input.kind) return json({ error: "unsupported_model" }, 400);
  if (prompt.length < 3 || prompt.length > 2000) return json({ error: "invalid_prompt" }, 400);
  if (idempotencyKey.length < 8 || idempotencyKey.length > 128) return json({ error: "invalid_idempotency_key" }, 400);

  let costs: Record<string, number>;
  try { costs = JSON.parse(costJson); } catch { return json({ error: "invalid_cost_configuration" }, 503); }
  const cost = costs[`${input.kind}:${input.model}`];
  if (!Number.isSafeInteger(cost) || cost < 1 || cost > 100000) {
    return json({ error: "model_cost_not_configured" }, 503);
  }

  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: reservation, error: reserveError } = await admin.rpc("reserve_generation", {
    p_user_id: authData.user.id,
    p_kind: input.kind,
    p_model_id: input.model,
    p_prompt: prompt,
    p_credits: cost,
    p_idempotency_key: idempotencyKey,
  });
  if (reserveError) {
    const message = reserveError.message ?? "";
    if (message.includes("insufficient_credits")) return json({ error: "insufficient_credits" }, 402);
    if (message.includes("account_unavailable")) return json({ error: "account_unavailable" }, 403);
    if (message.includes("duplicate_request")) return json({ error: "duplicate_request" }, 409);
    if (message.includes("rate_limited_hour")) return json({ error: "rate_limited" }, 429);
    if (message.includes("generation_concurrency_limit")) return json({ error: "too_many_active_generations" }, 429);
    return json({ error: "reservation_failed" }, 500);
  }
  const row = Array.isArray(reservation) ? reservation[0] : reservation;
  const jobId = row?.job_id;
  if (!jobId) return json({ error: "reservation_failed" }, 500);

  const endpoint = input.kind === "video"
    ? `https://gen.pollinations.ai/video/${encodeURIComponent(prompt)}?model=${encodeURIComponent(model.providerModel)}&duration=4`
    : `https://gen.pollinations.ai/image/${encodeURIComponent(prompt)}?model=${encodeURIComponent(model.providerModel)}&width=1024&height=1024&safe=true`;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), input.kind === "video" ? 115000 : 80000);
  try {
    const providerResponse = await fetch(endpoint, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${providerKey}`,
        "Accept": input.kind === "video" ? "video/mp4, application/json, */*" : "image/*, application/json",
      },
      signal: controller.signal,
    });
    if (!providerResponse.ok) {
      await admin.rpc("finish_generation", {
        p_user_id: authData.user.id, p_job_id: jobId, p_success: false,
        p_safe_error_code: `provider_http_${providerResponse.status}`,
      });
      return json({ error: "provider_failed", jobId }, 502);
    }
    let mediaResponse = providerResponse;
    let contentType = providerResponse.headers.get("content-type") ?? "application/octet-stream";
    if (contentType.includes("application/json")) {
      const providerJson = await providerResponse.json();
      const mediaUrl = providerJson?.url ?? providerJson?.video_url ?? providerJson?.image_url;
      if (typeof mediaUrl !== "string" || !mediaUrl.startsWith("https://")) {
        throw new Error("provider_response_invalid");
      }
      mediaResponse = await fetch(mediaUrl, { signal: controller.signal });
      if (!mediaResponse.ok) throw new Error("media_fetch_failed");
      contentType = mediaResponse.headers.get("content-type") ?? (input.kind === "video" ? "video/mp4" : "image/png");
    }
    const bytes = await mediaResponse.arrayBuffer();
    if (bytes.byteLength === 0 || bytes.byteLength > 40 * 1024 * 1024) throw new Error("media_size_invalid");
    await admin.rpc("finish_generation", {
      p_user_id: authData.user.id, p_job_id: jobId, p_success: true,
      p_media_url: null, p_safe_error_code: null,
    });
    return new Response(bytes, {
      status: 200,
      headers: {
        ...corsHeaders,
        "Cache-Control": "no-store",
        "Content-Type": contentType,
        "Content-Length": String(bytes.byteLength),
        "X-Generation-Job-Id": String(jobId),
        "X-Credits-Charged": String(cost),
      },
    });
  } catch (error) {
    await admin.rpc("finish_generation", {
      p_user_id: authData.user.id, p_job_id: jobId, p_success: false,
      p_safe_error_code: error instanceof DOMException && error.name === "AbortError" ? "provider_timeout" : "generation_failed",
    });
    return json({ error: "generation_failed", jobId }, 502);
  } finally {
    clearTimeout(timeout);
  }
});
