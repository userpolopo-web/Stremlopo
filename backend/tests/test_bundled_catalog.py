"""The shipped catalogue must work without an administrator entering titles or covers."""
import json, unittest
from pathlib import Path

class BundledCatalogTests(unittest.TestCase):
    def test_catalog_ready_on_first_launch_with_evidence_and_covers(self):
        path=Path(__file__).resolve().parents[2]/'android/app/src/main/assets/catalog.json'
        self.assertTrue(path.exists(), 'The initial catalogue is missing')
        films=json.loads(path.read_text())
        self.assertGreaterEqual(len(films),4)
        self.assertEqual(len({f['id'] for f in films}),len(films))
        for film in films:
            with self.subTest(title=film['title']):
                self.assertTrue(film['poster'].startswith('file:///android_asset/'))
                self.assertTrue((path.parent/film['poster'].removeprefix('file:///android_asset/')).is_file())
                self.assertTrue(film['rights']['evidence'].startswith('https://'))
                self.assertTrue(film['rights']['license'].startswith('CC BY'))
                self.assertTrue(film['attribution'])
                self.assertTrue(film['sources'])
                self.assertTrue(all(s['url'].startswith('https://') for s in film['sources']))
