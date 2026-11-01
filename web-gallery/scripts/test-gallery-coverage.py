#!/usr/bin/env python3
"""Regression checks for discoverable, reachable demos in both viewers."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("gallery", Path(__file__).with_name("generate-gallery.py"))
gallery = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gallery)


class CoverageTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        source, catalog, web = root / "library", root / "catalog", root / "web"
        for directory in (source, catalog, web / "src/wasmJsMain/kotlin"):
            directory.mkdir(parents=True)
        for name, value in (("SOURCE", source), ("CATALOG", catalog), ("GALLERY", web)):
            mocked = patch.object(gallery, name, value)
            mocked.start()
            self.addCleanup(mocked.stop)
        (source / "Widget.kt").write_text("@Composable\npublic fun Widget() {}")
        self.screen = catalog / "CatalogScreen.kt"
        self.screen.write_text('''
val demos = listOf(Demo("widget", "Widget", "Useful", Collection.Layout, "Widget"))
fun DemoContent(id: String) {
    when (id) { "widget" -> Widget() }
}
fun apiExample(id: String) = when (id) { "widget" -> "Widget()" }
fun App() { AnimatablesMotion() {} }
''')
        self.web = web / "src/wasmJsMain/kotlin/Gallery.kt"
        self.web.write_text('fun App() { AnimatablesMotion() { when (component) { "Widget" -> Widget() } } }')
        self.samples = [{"id": "Widget"}]

    def test_complete_viewers_pass(self):
        gallery.check_coverage(self.samples)

    def test_new_component_requires_both_demos(self):
        (gallery.SOURCE / "New.kt").write_text("@Composable\npublic fun New() {}")
        with self.assertRaisesRegex(ValueError, "missing=.*New"):
            gallery.check_coverage(self.samples)

    def test_orphaned_android_demo_fails_even_with_api_example(self):
        self.screen.write_text(self.screen.read_text().replace('when (id) { "widget" -> Widget() }', 'when (id) {}'))
        with self.assertRaisesRegex(ValueError, "registered, reachable"):
            gallery.check_coverage(self.samples)

    def test_unregistered_android_demo_fails(self):
        self.screen.write_text(self.screen.read_text().replace('Collection.Layout, "Widget"', 'Collection.Layout, "Other"'))
        with self.assertRaisesRegex(ValueError, "registered, reachable"):
            gallery.check_coverage(self.samples)

    def test_web_call_without_dispatch_fails(self):
        self.web.write_text('fun App() { AnimatablesMotion() { Widget() } }')
        with self.assertRaisesRegex(ValueError, "dispatch case"):
            gallery.check_coverage(self.samples)

    def test_duplicate_metadata_fails(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            gallery.check_coverage(self.samples * 2)


if __name__ == "__main__":
    unittest.main()
