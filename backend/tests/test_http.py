import json, os, sys, tempfile, threading, unittest, urllib.request, urllib.error
sys.path.insert(0,os.path.dirname(os.path.dirname(__file__)))
from server import Handler, ThreadingHTTPServer
from catalog import Catalog

class HttpTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(); self.token='a'*32
        self.old=os.environ.get('ADMIN_TOKEN');os.environ['ADMIN_TOKEN']=self.token
        self.server=ThreadingHTTPServer(('127.0.0.1',0),Handler);self.server.catalog=Catalog(self.tmp.name+'/test.db')
        self.thread=threading.Thread(target=self.server.serve_forever);self.thread.start()
        self.base='http://127.0.0.1:'+str(self.server.server_port)
    def tearDown(self):
        self.server.shutdown();self.thread.join();self.server.server_close();self.tmp.cleanup()
        if self.old is None: os.environ.pop('ADMIN_TOKEN',None)
        else: os.environ['ADMIN_TOKEN']=self.old
    def request(self,path,body=None,token=None):
        headers={'Content-Type':'application/json'}
        if token: headers['Authorization']='Bearer '+token
        req=urllib.request.Request(self.base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
        try:
            with urllib.request.urlopen(req) as r:return r.status,json.load(r)
        except urllib.error.HTTPError as e:return e.code,json.load(e)
    def test_admin_denied_without_token(self):
        self.assertEqual(self.request('/api/admin/content')[0],401)
        self.assertEqual(self.request('/api/admin/content',token='wrong')[0],401)
    def test_create_stays_out_of_public_catalog(self):
        status,item=self.request('/api/admin/content',{'title':'Teste','sources':[]},self.token)
        self.assertEqual(status,201);self.assertEqual(self.request('/api/catalog'),(200,[]))
        self.assertEqual(self.request('/api/admin/content/'+item['id']+'/publish',{},self.token)[0],400)
    def test_invalid_json_shape_rejected(self):
        self.assertEqual(self.request('/api/admin/content',[],self.token)[0],400)
    def test_health(self): self.assertEqual(self.request('/health'),(200,{'status':'ok'}))
