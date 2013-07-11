import BaseHTTPServer
from SocketServer import ThreadingMixIn
from urlparse import urlparse, parse_qs
import json
import multiprocessing
from parallelizer import  parallelize
from feature_extraction import classify

def classify_input_dict(email_data):
    try:
        return classify(email_data.get('email'), float(email_data.get('sent_count', 0)),
                    email_data.get('received_count', 0), email_data.get('name'))
    except:
        return {email_data.get("email"):0}

#@parallelize
def classify_list(emails):
    results = []
    for email in emails:
        results.append(classify_input_dict(email))
    return results

class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_GET(self):
        try:
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            path = self.path
            params = parse_qs(urlparse(path).query)
            email = params.get('email')
            if email:
                result = classify_input_dict(params)
                self.wfile.write(json.dumps(result))
            self.wfile.write("")
        except:
            self.wfile.write("")
        self.wfile.close()

    def do_POST(self):
        try:
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            data = self.rfile.readline()
            emails = json.loads(data)
            results = classify_list(emails)
            self.wfile.write(json.dumps(results))
            self.wfile.write("")
        except:
            self.wfile.write("")
        self.wfile.close()

class MultiThreadedHTTPServer(ThreadingMixIn, BaseHTTPServer.HTTPServer):
    pass

def run(server_class=MultiThreadedHTTPServer,
        handler_class=BaseHTTPServer.BaseHTTPRequestHandler):
    server_address = ('0.0.0.0', 8080)
    httpd = server_class(server_address, handler_class)
    httpd.serve_forever()

if __name__ == '__main__':
    run(handler_class=Handler)

