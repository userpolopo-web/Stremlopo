"""Development server. Deploy behind an HTTPS reverse proxy; no video proxying."""
import hmac, json, os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
from catalog import Catalog

class Handler(BaseHTTPRequestHandler):
    def respond(self,status,data):
        body=json.dumps(data,ensure_ascii=False).encode()
        self.send_response(status); self.send_header('Content-Type','application/json; charset=utf-8'); self.send_header('Cache-Control','no-store'); self.send_header('X-Content-Type-Options','nosniff'); self.end_headers(); self.wfile.write(body)
    def authorized(self):
        token=os.environ.get('ADMIN_TOKEN','')
        return len(token)>=32 and hmac.compare_digest(self.headers.get('Authorization',''),'Bearer '+token)
    def handle_request(self,method):
        path=urlsplit(self.path).path; parts=path.strip('/').split('/')
        if path.startswith('/api/admin') and not self.authorized(): return self.respond(401,{'error':'Não autorizado'})
        try:
            payload={}
            if method in ('POST','PUT'):
                size=int(self.headers.get('Content-Length','0'))
                if size>65536: return self.respond(413,{'error':'Requisição grande demais'})
                payload=json.loads(self.rfile.read(size) or b'{}')
                if not isinstance(payload,dict): raise ValueError('Objeto JSON obrigatório')
            c=self.server.catalog
            if method=='GET' and path=='/health': return self.respond(200,{'status':'ok'})
            if method=='GET' and path=='/api/catalog': return self.respond(200,c.list_public())
            if method=='GET' and path=='/api/admin/content': return self.respond(200,c.list_all())
            if method=='GET' and path=='/api/admin/reports': return self.respond(200,c.reports())
            if method=='POST' and path=='/api/admin/content': return self.respond(201,c.save(payload))
            if method=='POST' and len(parts)==5 and parts[:3]==['api','admin','content'] and parts[4]=='publish': return self.respond(200,c.publish(parts[3]))
            if method=='DELETE' and len(parts)==4 and parts[:3]==['api','admin','content']:
                c.remove(parts[3]); return self.respond(200,{'removed':True})
            if method=='POST' and path=='/api/reports': return self.respond(201,c.report(payload['content_id'],payload['reason']))
            if method=='GET' and path=='/admin':
                body=Path(__file__).with_name('admin.html').read_bytes(); self.send_response(200); self.send_header('Content-Type','text/html; charset=utf-8'); self.send_header('X-Frame-Options','DENY'); self.end_headers(); self.wfile.write(body); return
            self.respond(404,{'error':'Não encontrado'})
        except KeyError: self.respond(404,{'error':'Conteúdo não encontrado ou campo obrigatório ausente'})
        except (ValueError,TypeError): self.respond(400,{'error':'Dados inválidos; confira as fontes e os registros de licença.'})
    def do_GET(self): self.handle_request('GET')
    def do_POST(self): self.handle_request('POST')
    def do_DELETE(self): self.handle_request('DELETE')

if __name__=='__main__':
    if len(os.environ.get('ADMIN_TOKEN',''))<32: raise SystemExit('Defina ADMIN_TOKEN com pelo menos 32 caracteres.')
    server=ThreadingHTTPServer((os.getenv('HOST','127.0.0.1'),int(os.getenv('PORT','8080'))),Handler)
    server.catalog=Catalog(os.getenv('DATABASE_PATH','catalog.db'))
    print('Stremlopo: http://127.0.0.1:8080/admin'); server.serve_forever()
