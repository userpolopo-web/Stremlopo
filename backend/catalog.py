"""Persistent catalogue. Every edit revokes publication and requires a new review."""
import ipaddress, json, sqlite3, uuid
from datetime import datetime, timezone
from urllib.parse import urlsplit

def validate_video_url(url):
    p=urlsplit(url)
    if p.scheme!='https' or not p.hostname or p.username or p.password:
        raise ValueError('A fonte deve usar HTTPS sem credenciais na URL.')
    host=p.hostname.lower()
    if host=='localhost' or '.' not in host or host.endswith(('.local','.internal')):
        raise ValueError('Fontes locais não são permitidas.')
    try:
        if not ipaddress.ip_address(host).is_global: raise ValueError('IP privado não permitido.')
    except ValueError as e:
        if 'IP privado' in str(e): raise
    return url

class Catalog:
    def __init__(self, path):
        self.path=path
        with self.db() as db:
            db.executescript('CREATE TABLE IF NOT EXISTS content(id TEXT PRIMARY KEY, data TEXT NOT NULL); CREATE TABLE IF NOT EXISTS reports(id TEXT PRIMARY KEY, data TEXT NOT NULL); CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY, content_id TEXT, action TEXT, at TEXT);')
    def db(self): return sqlite3.connect(self.path)
    def audit(self, db, id, action): db.execute('INSERT INTO audit(content_id,action,at) VALUES(?,?,?)',(id,action,datetime.now(timezone.utc).isoformat()))
    def save(self, data):
        item=dict(data)
        if not str(item.get('title','')).strip(): raise ValueError('Título obrigatório.')
        if item.get('kind','movie') not in ('movie','series','episode'): raise ValueError('Tipo inválido.')
        sources=item.get('sources',[])
        if not isinstance(sources,list): raise ValueError('Fontes inválidas.')
        for source in sources:
            validate_video_url(source['url'])
            if source.get('format') not in ('mp4','hls'): raise ValueError('Formato deve ser mp4 ou hls.')
        item['id']=item.get('id') or str(uuid.uuid4()); item['published']=False
        item['updated_at']=datetime.now(timezone.utc).isoformat()
        with self.db() as db:
            db.execute('INSERT OR REPLACE INTO content VALUES(?,?)',(item['id'],json.dumps(item)))
            self.audit(db,item['id'],'save:publication-revoked')
        return item
    def get(self,id):
        with self.db() as db: row=db.execute('SELECT data FROM content WHERE id=?',(id,)).fetchone()
        if not row: raise KeyError(id)
        return json.loads(row[0])
    def list_all(self):
        with self.db() as db: return [json.loads(r[0]) for r in db.execute('SELECT data FROM content ORDER BY rowid DESC')]
    def list_public(self):
        # Evidence and reviewer identities remain private.
        return [{k:v for k,v in x.items() if k!='rights'} | {'attribution':x.get('rights',{}).get('attribution',''),'license':x.get('rights',{}).get('license','')} for x in self.list_all() if x.get('published')]
    def publish(self,id):
        item=self.get(id); r=item.get('rights',{})
        if not all(r.get(k) is True for k in ('verified','embedded_allowed')) or not all(str(r.get(k,'')).strip() for k in ('holder','license','evidence','verified_by','attribution')):
            raise ValueError('Registre titular, licença, evidência, créditos e responsável; confirme a autorização de reprodução integrada.')
        if not item.get('sources'): raise ValueError('Cadastre uma fonte autorizada.')
        if item.get('monetized') and r.get('commercial_allowed') is not True: raise ValueError('Licença sem permissão comercial.')
        item['published']=True
        with self.db() as db:
            db.execute('UPDATE content SET data=? WHERE id=?',(json.dumps(item),id)); self.audit(db,id,'publish:manual-rights-review')
        return item
    def remove(self,id):
        self.get(id)
        with self.db() as db: db.execute('DELETE FROM content WHERE id=?',(id,)); self.audit(db,id,'remove')
    def report(self,id,reason):
        self.get(id)
        if not reason.strip() or len(reason)>2000: raise ValueError('Descreva a denúncia em até 2000 caracteres.')
        data=dict(id=str(uuid.uuid4()),content_id=id,reason=reason,at=datetime.now(timezone.utc).isoformat())
        with self.db() as db: db.execute('INSERT INTO reports VALUES(?,?)',(data['id'],json.dumps(data)))
        return data
    def reports(self):
        with self.db() as db: return [json.loads(x[0]) for x in db.execute('SELECT data FROM reports')]
