import os, tempfile, unittest, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from catalog import Catalog, validate_video_url

class CatalogTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.c = Catalog(self.tmp.name + '/test.db')
        self.item = dict(title='Filme de teste', synopsis='Descrição', genre='Animação', year=2020, duration=60, poster='', kind='movie', sources=[{'url':'https://media.example.org/movie.mp4','format':'mp4'}], rights={})
    def tearDown(self): self.tmp.cleanup()
    def test_draft_hidden_and_unverified_publication_blocked(self):
        item = self.c.save(self.item)
        self.assertEqual(self.c.list_public(), [])
        with self.assertRaises(ValueError): self.c.publish(item['id'])
    def approved(self):
        self.item['rights'] = dict(holder='Titular', license='CC BY 4.0', evidence='https://example.org/license', attribution='Créditos', verified_by='Administrador', verified=True, embedded_allowed=True, commercial_allowed=False)
        return self.c.save(self.item)
    def test_approved_work_published_and_edit_revokes_publication(self):
        item = self.approved(); self.c.publish(item['id'])
        self.assertEqual(len(self.c.list_public()), 1)
        self.c.save({**item, 'title':'Alterado'})
        self.assertEqual(self.c.list_public(), [])
    def test_report_and_removal(self):
        item=self.approved(); self.c.publish(item['id'])
        report=self.c.report(item['id'], 'Sou titular desta obra')
        self.assertEqual(report['content_id'],item['id'])
        self.c.remove(item['id']); self.assertEqual(self.c.list_public(),[])
    def test_private_and_invalid_video_urls_rejected(self):
        for url in ['http://example.org/a.mp4','https://127.0.0.1/a.mp4','https://localhost/a.mp4','https://user:pass@example.org/a.mp4','file:///a.mp4']:
            with self.subTest(url=url), self.assertRaises(ValueError): validate_video_url(url)
    def test_monetization_requires_permission(self):
        item=self.approved(); item['monetized']=True; item=self.c.save(item)
        with self.assertRaises(ValueError): self.c.publish(item['id'])

if __name__=='__main__': unittest.main()
