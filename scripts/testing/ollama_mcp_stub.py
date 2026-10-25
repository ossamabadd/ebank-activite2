import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ACCOUNT_ID = "11111111-1111-1111-1111-111111111111"


def find_account(value):
    if isinstance(value, str):
        try:
            return find_account(json.loads(value))
        except (ValueError, TypeError):
            return None
    if isinstance(value, dict):
        if "balance" in value and "currency" in value:
            return value
        for child in value.values():
            result = find_account(child)
            if result:
                return result
    if isinstance(value, list):
        for child in value:
            result = find_account(child)
            if result:
                return result
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=11435)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def do_POST(self):
            if self.path != "/api/chat":
                self.send_error(404)
                return
            if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
                chunks = []
                while True:
                    size = int(self.rfile.readline().split(b";", 1)[0].strip(), 16)
                    if size == 0:
                        while self.rfile.readline().strip():
                            pass
                        break
                    chunks.append(self.rfile.read(size))
                    self.rfile.read(2)
                body = b"".join(chunks)
            else:
                body = self.rfile.read(int(self.headers["Content-Length"]))
            request = json.loads(body)
            tool_messages = [m for m in request["messages"] if m["role"] == "tool"]
            if tool_messages:
                account = find_account(tool_messages[-1]["content"])
                if not account:
                    self.send_error(500, "MCP did not return account data")
                    return
                answer = f"Test simule — solde retourne par MCP : {account['balance']:.2f} {account['currency']}"
                message = {"role": "assistant", "content": answer}
                if args.report:
                    args.report.write_text(json.dumps({
                        "simulated_model": True, "real_mcp_result": account,
                        "tools_exposed_to_model": [t["function"]["name"] for t in request["tools"]],
                        "answer": answer,
                    }, ensure_ascii=False, indent=2), encoding="utf-8")
            else:
                tool = next(t["function"]["name"] for t in request["tools"]
                            if t["function"]["name"].endswith("getAccount"))
                message = {"role": "assistant", "content": "", "tool_calls": [
                    {"function": {"name": tool, "arguments": {"accountId": ACCOUNT_ID}}}
                ]}
            response = json.dumps({
                "model": request["model"], "created_at": "2026-10-20T09:00:00Z",
                "message": message, "done": True,
            }, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()
            self.wfile.write(response)

    print(f"Ollama TEST DOUBLE listening on 127.0.0.1:{args.port}", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
