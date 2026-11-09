#!/usr/bin/env bash
# NVIDIA (build.nvidia.com): the models its API lists, and how it answers without a key
set -u
curl -s -m 30 https://integrate.api.nvidia.com/v1/models > /tmp/nv.json
python3 - <<'PY'
import json
d = json.load(open('/tmp/nv.json'))
ids = [m['id'] for m in d.get('data', [])]
print('COUNT', len(ids))
for i in ids: print('MODEL', i)
PY
echo "--- chat with a bad key:"
curl -s -m 30 -o - -w "\nHTTP %{http_code}\n" https://integrate.api.nvidia.com/v1/chat/completions \
  -H 'authorization: Bearer nvapi-bad' -H 'content-type: application/json' \
  -d '{"model":"meta/llama-3.3-70b-instruct","messages":[{"role":"user","content":"hi"}],"max_tokens":5}'
echo "--- models with a bad key:"
curl -s -m 30 -o /dev/null -w "HTTP %{http_code}\n" https://integrate.api.nvidia.com/v1/models -H 'authorization: Bearer nvapi-bad'
