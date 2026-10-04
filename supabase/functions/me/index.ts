import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
};
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "GET") return Response.json({ error: "method_not_allowed" }, { status: 405, headers: corsHeaders });
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const authHeader = req.headers.get("Authorization");
  if (!supabaseUrl || !anonKey) return Response.json({ error: "backend_not_configured" }, { status: 503, headers: corsHeaders });
  if (!authHeader?.startsWith("Bearer ")) return Response.json({ error: "unauthorized" }, { status: 401, headers: corsHeaders });
  const client = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await client.auth.getUser();
  if (userError || !userData.user) return Response.json({ error: "unauthorized" }, { status: 401, headers: corsHeaders });
  const [{ data: profile, error: profileError }, { data: balance, error: balanceError }] = await Promise.all([
    client.from("profiles").select("id,display_name,status,created_at").eq("id", userData.user.id).maybeSingle(),
    client.rpc("get_my_credit_balance"),
  ]);
  if (profileError || balanceError) return Response.json({ error: "profile_unavailable" }, { status: 500, headers: corsHeaders });
  return Response.json({
    user: { id: userData.user.id, email: userData.user.email ?? null },
    profile, creditBalance: Number(balance ?? 0),
  }, { headers: { ...corsHeaders, "Cache-Control": "no-store" } });
});
