#!/usr/bin/env bash
# E2E smoke test for Social Media Search API
# Prerequisites: app running on localhost:8080, Elasticsearch on localhost:9200
# Usage: ./scripts/e2e-test.sh

set -uo pipefail

BASE_URL="http://localhost:8080/api/v1/search"
USER="atulk"
PASS="password123"
PASS_WRONG="wrongpass"

PASS_COUNT=0
FAIL_COUNT=0

pass() { echo "  PASS  $1"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { echo "  FAIL  $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

# Run curl; sets BODY and STATUS
do_request() {
  local auth_args=("$@")
  BODY=$(curl -s "${auth_args[@]}")
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${auth_args[@]}")
}

assert_status() {
  local label="$1" expected="$2"
  if [[ "$STATUS" == "$expected" ]]; then
    pass "$label (HTTP $STATUS)"
  else
    fail "$label — expected HTTP $expected, got $STATUS"
  fi
}

assert_json() {
  local label="$1" expected="$2" expr="$3"
  local actual
  actual=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print($expr)" 2>/dev/null || echo "")
  if [[ "$actual" == "$expected" ]]; then
    pass "$label"
  else
    fail "$label — expected '$expected', got '$actual'"
  fi
}

assert_json_gt() {
  local label="$1" expr="$2"
  local actual
  actual=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print($expr)" 2>/dev/null || echo "0")
  if [[ "$actual" -gt 0 ]]; then
    pass "$label (value=$actual)"
  else
    fail "$label — expected > 0, got $actual"
  fi
}

echo ""
echo "========================================"
echo " Social Media Search — E2E Test Suite"
echo "========================================"
echo ""

# ── Search: USER ──────────────────────────────────────────────
echo "[1] Search USER by name/username"
do_request -u "$USER:$PASS" "$BASE_URL?q=atul&type=USER&size=10"
assert_status "USER search returns 200" "200"
assert_json_gt "USER search has results" "d['data']['totalHits']"

# ── Search: POST ──────────────────────────────────────────────
echo ""
echo "[2] Search POST by caption"
do_request -u "$USER:$PASS" "$BASE_URL?q=spring&type=POST&size=10"
assert_status "POST search returns 200" "200"
assert_json_gt "POST search has results" "d['data']['totalHits']"

# ── Search: PAGE ──────────────────────────────────────────────
echo ""
echo "[3] Search PAGE by name"
do_request -u "$USER:$PASS" "$BASE_URL?q=java&type=PAGE&size=10"
assert_status "PAGE search returns 200" "200"
assert_json_gt "PAGE search has results" "d['data']['totalHits']"

# ── Search: TAG ───────────────────────────────────────────────
echo ""
echo "[4] Search TAG by name"
do_request -u "$USER:$PASS" "$BASE_URL?q=microservices&type=TAG&size=10"
assert_status "TAG search returns 200" "200"
assert_json_gt "TAG search has results" "d['data']['totalHits']"

# ── Search: LOCATION ──────────────────────────────────────────
echo ""
echo "[5] Search LOCATION by display name"
do_request -u "$USER:$PASS" "$BASE_URL?q=bengaluru&type=LOCATION&size=10"
assert_status "LOCATION search returns 200" "200"
assert_json_gt "LOCATION search has results" "d['data']['totalHits']"

# ── Auth: No credentials ──────────────────────────────────────
echo ""
echo "[6] Auth — no credentials"
do_request "$BASE_URL?q=atul&type=USER"
assert_status "No credentials returns 401" "401"
assert_json "error.code is UNAUTHORIZED" "UNAUTHORIZED" "d['error']['code']"
assert_json "error.message is 'Authentication required'" "Authentication required" "d['error']['message']"

# ── Auth: Wrong password ──────────────────────────────────────
echo ""
echo "[7] Auth — wrong password"
do_request -u "$USER:$PASS_WRONG" "$BASE_URL?q=atul&type=USER"
assert_status "Wrong password returns 401" "401"
assert_json "error.code is UNAUTHORIZED" "UNAUTHORIZED" "d['error']['code']"
assert_json "error.message is 'password incorrect'" "password incorrect" "d['error']['message']"

# ── Validation: blank query ────────────────────────────────────
echo ""
echo "[8] Validation — blank query"
do_request -u "$USER:$PASS" "$BASE_URL?q=%20&type=USER"
assert_status "Blank query returns 400" "400"
assert_json "error.code is VALIDATION_ERROR" "VALIDATION_ERROR" "d['error']['code']"

# ── Validation: invalid type ──────────────────────────────────
echo ""
echo "[9] Validation — invalid type"
do_request -u "$USER:$PASS" "$BASE_URL?q=atul&type=INVALID"
assert_status "Invalid type returns 400" "400"
assert_json "error.code is BAD_REQUEST" "BAD_REQUEST" "d['error']['code']"

# ── Pagination ────────────────────────────────────────────────
echo ""
echo "[10] Pagination — custom page size"
do_request -u "$USER:$PASS" "$BASE_URL?q=spring&type=POST&size=1"
assert_status "Pagination returns 200" "200"
assert_json "Page size is 1" "1" "d['data']['size']"

# ── Pagination: cursor ────────────────────────────────────────
echo ""
echo "[11] Pagination — cursor-based search_after"
do_request -u "$USER:$PASS" "$BASE_URL?q=a&type=USER&size=1"
assert_status "First page returns 200" "200"
CURSOR=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'].get('nextCursor') or '')" 2>/dev/null || echo "")
if [[ -n "$CURSOR" ]]; then
  pass "First page returned a nextCursor"
  do_request -u "$USER:$PASS" "$BASE_URL?q=a&type=USER&size=1&cursor=$CURSOR"
  assert_status "Cursor follow-up returns 200" "200"
else
  pass "First page had no nextCursor (no more results)"
fi

# ── Pagination: bad cursor ────────────────────────────────────
echo ""
echo "[12] Pagination — invalid cursor"
do_request -u "$USER:$PASS" "$BASE_URL?q=atul&type=USER&cursor=not-base64!!!"
assert_status "Bad cursor returns 400" "400"
assert_json "error.code is BAD_CURSOR" "BAD_CURSOR" "d['error']['code']"

# ── Summary ───────────────────────────────────────────────────
echo ""
echo "========================================"
TOTAL_TESTS=$((PASS_COUNT + FAIL_COUNT))
echo " Results: $PASS_COUNT/$TOTAL_TESTS passed"
if [[ "$FAIL_COUNT" -gt 0 ]]; then
  echo " $FAIL_COUNT test(s) FAILED"
  echo "========================================"
  echo ""
  exit 1
else
  echo " All tests passed"
  echo "========================================"
  echo ""
  exit 0
fi
