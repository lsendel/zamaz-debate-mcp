#!/usr/bin/env python3
"""
Comprehensive Documentation Analysis System

This module provides a complete documentation analysis system that includes:
1. Multi-format documentation parsing (Markdown, AsciiDoc, RST, HTML)
2. Code-documentation linking and cross-referencing
3. Documentation quality analysis and scoring
4. API documentation extraction from code comments
5. Documentation coverage analysis
6. Documentation search and indexing with semantic understanding
7. Outdated documentation detection
8. Documentation generation recommendations
9. Integration with existing repository analysis
10. Support for multiple documentation tools (Javadoc, Sphinx, GitBook)
"""

import ast
import contextlib
import datetime
import json
import logging
import os
import re
import subprocess
from abc import ABC, abstractmethod
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any

import yaml

# Third-party imports (these would need to be installed)
try:
    import ast

    import docutils.core
    import docutils.parsers.rst  # noqa: F401
    import javalang
    import markdown
    import mistune  # noqa: F401
    import nltk  # noqa: F401
    import numpy as np  # noqa: F401
    import spacy  # noqa: F401
    from asciidoc import AsciiDocAPI
    from bs4 import BeautifulSoup
    from nltk.corpus import stopwords
    from nltk.stem import WordNetLemmatizer
    from nltk.tokenize import sent_tokenize, word_tokenize
    from pygments import highlight  # noqa: F401
    from pygments.formatters import HtmlFormatter  # noqa: F401
    from pygments.lexers import get_lexer_by_name, guess_lexer  # noqa: F401
    from sentence_transformers import SentenceTransformer  # noqa: F401
    from sklearn.cluster import KMeans  # noqa: F401
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.metrics.pairwise import cosine_similarity

    OPTIONAL_IMPORTS = True
except ImportError:
    OPTIONAL_IMPORTS = False

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class DocumentationFile:
    """Represents a documentation file with metadata and content."""

    path: str
    format: str
    title: str
    content: str
    headers: list[str] = field(default_factory=list)
    links: list[str] = field(default_factory=list)
    images: list[str] = field(default_factory=list)
    code_blocks: list[dict[str, str]] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    last_modified: datetime.datetime = field(default_factory=datetime.datetime.now)
    size: int = 0
    line_count: int = 0
    word_count: int = 0
    readability_score: float = 0.0
    completeness_score: float = 0.0
    freshness_score: float = 0.0
    quality_score: float = 0.0
    cross_references: list[str] = field(default_factory=list)
    api_references: list[str] = field(default_factory=list)
    examples: list[dict[str, str]] = field(default_factory=list)
    todos: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


@dataclass
class CodeFile:
    """Represents a code file with documentation extraction."""

    path: str
    language: str
    classes: list[dict[str, Any]] = field(default_factory=list)
    functions: list[dict[str, Any]] = field(default_factory=list)
    comments: list[dict[str, Any]] = field(default_factory=list)
    docstrings: list[dict[str, Any]] = field(default_factory=list)
    imports: list[str] = field(default_factory=list)
    annotations: list[dict[str, Any]] = field(default_factory=list)
    api_endpoints: list[dict[str, Any]] = field(default_factory=list)
    javadoc_comments: list[dict[str, Any]] = field(default_factory=list)
    swagger_annotations: list[dict[str, Any]] = field(default_factory=list)
    documentation_coverage: float = 0.0


@dataclass
class DocumentationIssue:
    """Represents an issue found in documentation."""

    severity: str  # 'error', 'warning', 'info'
    type: str
    message: str
    file_path: str
    line_number: int | None = None
    suggestion: str | None = None


@dataclass
class DocumentationMetrics:
    """Comprehensive documentation metrics."""

    total_files: int = 0
    total_words: int = 0
    total_lines: int = 0
    coverage_percentage: float = 0.0
    quality_score: float = 0.0
    freshness_score: float = 0.0
    completeness_score: float = 0.0
    cross_reference_count: int = 0
    broken_links: int = 0
    outdated_content: int = 0
    missing_api_docs: int = 0
    documentation_debt: float = 0.0
    formats: dict[str, int] = field(default_factory=dict)
    categories: dict[str, int] = field(default_factory=dict)
    issues: list[DocumentationIssue] = field(default_factory=list)


class DocumentationParser(ABC):
    """Abstract base class for documentation parsers."""

    @abstractmethod
    def parse(self, content: str, file_path: str) -> DocumentationFile:
        """Parse documentation content and return structured data."""
        pass

    @abstractmethod
    def extract_metadata(self, content: str) -> dict[str, Any]:
        """Extract metadata from documentation content."""
        pass


class MarkdownParser(DocumentationParser):
    """Parser for Markdown documentation files."""

    def __init__(self):
        self.md_processor = None
        if OPTIONAL_IMPORTS:
            self.md_processor = markdown.Markdown(extensions=["meta", "toc", "tables", "fenced_code", "codehilite"])

    def parse(self, content: str, file_path: str) -> DocumentationFile:
        """Parse Markdown content."""
        doc = DocumentationFile(
            path=file_path,
            format="markdown",
            content=content,
            title=self._extract_title(content),
            size=len(content),
            line_count=len(content.split("\n")),
            word_count=len(content.split()),
        )

        # Extract headers
        doc.headers = self._extract_headers(content)

        # Extract links
        doc.links = self._extract_links(content)

        # Extract images
        doc.images = self._extract_images(content)

        # Extract code blocks
        doc.code_blocks = self._extract_code_blocks(content)

        # Extract metadata
        doc.metadata = self.extract_metadata(content)

        # Extract cross-references
        doc.cross_references = self._extract_cross_references(content)

        # Extract examples
        doc.examples = self._extract_examples(content)

        # Extract TODOs and warnings
        doc.todos = self._extract_todos(content)
        doc.warnings = self._extract_warnings(content)

        return doc

    def extract_metadata(self, content: str) -> dict[str, Any]:
        """Extract YAML front matter and other metadata."""
        metadata = {}

        # Extract YAML front matter
        if content.startswith("---"):
            try:
                end_idx = content.find("---", 3)
                if end_idx != -1:
                    yaml_content = content[3:end_idx]
                    metadata = yaml.safe_load(yaml_content) or {}
            except yaml.YAMLError:
                pass

        return metadata

    def _extract_title(self, content: str) -> str:
        """Extract title from content."""
        lines = content.split("\n")
        for line in lines:
            if line.startswith("# "):
                return line[2:].strip()

        # Try to extract from filename if content doesn't have a title
        return "Untitled"

    def _extract_headers(self, content: str) -> list[str]:
        """Extract all headers from markdown content."""
        headers = []
        for line in content.split("\n"):
            if line.startswith("#"):
                headers.append(line.strip())
        return headers

    def _extract_links(self, content: str) -> list[str]:
        """Extract all links from markdown content."""
        # Pattern for markdown links: [text](url)
        link_pattern = r"\[([^\]]+)\]\(([^)]+)\)"
        links = re.findall(link_pattern, content)
        return [url for text, url in links]

    def _extract_images(self, content: str) -> list[str]:
        """Extract all images from markdown content."""
        # Pattern for markdown images: ![alt](url)
        image_pattern = r"!\[([^\]]*)\]\(([^)]+)\)"
        images = re.findall(image_pattern, content)
        return [url for alt, url in images]

    def _extract_code_blocks(self, content: str) -> list[dict[str, str]]:
        """Extract code blocks from markdown content."""
        code_blocks = []

        # Pattern for fenced code blocks
        fenced_pattern = r"```(\w+)?\n(.*?)\n```"
        matches = re.findall(fenced_pattern, content, re.DOTALL)

        for language, code in matches:
            code_blocks.append({"language": language or "text", "code": code.strip()})

        # Pattern for indented code blocks
        indented_pattern = r"\n( {4}.*(?:\n {4}.*)*)"
        indented_matches = re.findall(indented_pattern, content)

        for code in indented_matches:
            code_blocks.append({"language": "text", "code": code.strip()})

        return code_blocks

    def _extract_cross_references(self, content: str) -> list[str]:
        """Extract cross-references to other documentation."""
        refs = []

        # Look for references to other files
        ref_patterns = [
            r"\[([^\]]+)\]\(([^)]+\.md[^)]*)\)",  # Markdown file links
            r"see\s+([A-Za-z0-9_.-]+\.md)",  # "see filename.md" pattern
            r"@see\s+([A-Za-z0-9_.-]+)",  # Javadoc-style @see
        ]

        for pattern in ref_patterns:
            matches = re.findall(pattern, content, re.IGNORECASE)
            refs.extend(matches if isinstance(matches[0], str) else [m[1] for m in matches])

        return refs

    def _extract_examples(self, content: str) -> list[dict[str, str]]:
        """Extract examples from documentation."""
        examples = []

        # Look for example sections
        example_pattern = r"## Example[s]?\s*\n(.*?)(?=\n##|\Z)"
        matches = re.findall(example_pattern, content, re.DOTALL | re.IGNORECASE)

        for example in matches:
            examples.append({"type": "example", "content": example.strip()})

        return examples

    def _extract_todos(self, content: str) -> list[str]:
        """Extract TODO items from documentation."""
        todo_pattern = r"(?:TODO|FIXME|XXX)[:\s]+(.*?)(?:\n|$)"
        return re.findall(todo_pattern, content, re.IGNORECASE)

    def _extract_warnings(self, content: str) -> list[str]:
        """Extract warnings and notes from documentation."""
        warning_patterns = [
            r"(?:WARNING|CAUTION|NOTE)[:\s]+(.*?)(?:\n|$)",
            r"> \*\*Warning\*\*[:\s]+(.*?)(?:\n|$)",
            r'{% hint style="warning" %}(.*?){% endhint %}',
        ]

        warnings = []
        for pattern in warning_patterns:
            matches = re.findall(pattern, content, re.IGNORECASE | re.DOTALL)
            warnings.extend(matches)

        return warnings


class AsciiDocParser(DocumentationParser):
    """Parser for AsciiDoc documentation files."""

    def __init__(self):
        self.asciidoc_api = None
        if OPTIONAL_IMPORTS:
            with contextlib.suppress(Exception):
                self.asciidoc_api = AsciiDocAPI()

    def parse(self, content: str, file_path: str) -> DocumentationFile:
        """Parse AsciiDoc content."""
        doc = DocumentationFile(
            path=file_path,
            format="asciidoc",
            content=content,
            title=self._extract_title(content),
            size=len(content),
            line_count=len(content.split("\n")),
            word_count=len(content.split()),
        )

        # Extract headers
        doc.headers = self._extract_headers(content)

        # Extract links
        doc.links = self._extract_links(content)

        # Extract code blocks
        doc.code_blocks = self._extract_code_blocks(content)

        # Extract metadata
        doc.metadata = self.extract_metadata(content)

        return doc

    def extract_metadata(self, content: str) -> dict[str, Any]:
        """Extract AsciiDoc metadata."""
        metadata = {}

        # Extract document attributes
        attr_pattern = r"^:([^:]+):\s*(.*)$"
        for line in content.split("\n"):
            match = re.match(attr_pattern, line)
            if match:
                key, value = match.groups()
                metadata[key.strip()] = value.strip()

        return metadata

    def _extract_title(self, content: str) -> str:
        """Extract title from AsciiDoc content."""
        lines = content.split("\n")
        for line in lines:
            if line.startswith("= "):
                return line[2:].strip()
        return "Untitled"

    def _extract_headers(self, content: str) -> list[str]:
        """Extract headers from AsciiDoc content."""
        headers = []
        for line in content.split("\n"):
            if re.match(r"^=+\s", line):
                headers.append(line.strip())
        return headers

    def _extract_links(self, content: str) -> list[str]:
        """Extract links from AsciiDoc content."""
        # Pattern for AsciiDoc links: link:url[text]
        link_pattern = r"link:([^[]+)\[([^\]]*)\]"
        links = re.findall(link_pattern, content)
        return [url for url, text in links]

    def _extract_code_blocks(self, content: str) -> list[dict[str, str]]:
        """Extract code blocks from AsciiDoc content."""
        code_blocks = []

        # Pattern for AsciiDoc code blocks
        code_pattern = r"\[source,(\w+)\]\n----\n(.*?)\n----"
        matches = re.findall(code_pattern, content, re.DOTALL)

        for language, code in matches:
            code_blocks.append({"language": language, "code": code.strip()})

        return code_blocks


class RestructuredTextParser(DocumentationParser):
    """Parser for reStructuredText documentation files."""

    def parse(self, content: str, file_path: str) -> DocumentationFile:
        """Parse reStructuredText content."""
        doc = DocumentationFile(
            path=file_path,
            format="rst",
            content=content,
            title=self._extract_title(content),
            size=len(content),
            line_count=len(content.split("\n")),
            word_count=len(content.split()),
        )

        # Extract headers
        doc.headers = self._extract_headers(content)

        # Extract links
        doc.links = self._extract_links(content)

        # Extract code blocks
        doc.code_blocks = self._extract_code_blocks(content)

        # Extract metadata
        doc.metadata = self.extract_metadata(content)

        return doc

    def extract_metadata(self, content: str) -> dict[str, Any]:
        """Extract RST metadata."""
        metadata = {}

        # Extract document info
        info_pattern = r"^:([^:]+):\s*(.*)$"
        for line in content.split("\n"):
            match = re.match(info_pattern, line)
            if match:
                key, value = match.groups()
                metadata[key.strip()] = value.strip()

        return metadata

    def _extract_title(self, content: str) -> str:
        """Extract title from RST content."""
        lines = content.split("\n")
        for i, line in enumerate(lines):
            if i + 1 < len(lines) and re.match(r"^=+$", lines[i + 1]):
                return line.strip()
        return "Untitled"

    def _extract_headers(self, content: str) -> list[str]:
        """Extract headers from RST content."""
        headers = []
        lines = content.split("\n")

        for i, line in enumerate(lines):
            if i + 1 < len(lines):
                next_line = lines[i + 1]
                if re.match(r'^[=\-`:\'"~^_*+#<>]+$', next_line) and len(next_line) >= len(line):
                    headers.append(line.strip())

        return headers

    def _extract_links(self, content: str) -> list[str]:
        """Extract links from RST content."""
        # Pattern for RST links: `text <url>`_
        link_pattern = r"`([^<]+)<([^>]+)>`_"
        links = re.findall(link_pattern, content)
        return [url for text, url in links]

    def _extract_code_blocks(self, content: str) -> list[dict[str, str]]:
        """Extract code blocks from RST content."""
        code_blocks = []

        # Pattern for RST code blocks
        code_pattern = r"\.\. code-block:: (\w+)\n\n((?:    .*\n)*)"
        matches = re.findall(code_pattern, content)

        for language, code in matches:
            code_blocks.append({"language": language, "code": code.strip()})

        return code_blocks


class HtmlParser(DocumentationParser):
    """Parser for HTML documentation files."""

    def parse(self, content: str, file_path: str) -> DocumentationFile:
        """Parse HTML content."""
        doc = DocumentationFile(
            path=file_path,
            format="html",
            content=content,
            title=self._extract_title(content),
            size=len(content),
            line_count=len(content.split("\n")),
            word_count=len(self._extract_text(content).split()),
        )

        if OPTIONAL_IMPORTS:
            soup = BeautifulSoup(content, "html.parser")

            # Extract headers
            doc.headers = self._extract_headers(soup)

            # Extract links
            doc.links = self._extract_links(soup)

            # Extract images
            doc.images = self._extract_images(soup)

            # Extract code blocks
            doc.code_blocks = self._extract_code_blocks(soup)

            # Extract metadata
            doc.metadata = self.extract_metadata(soup)

        return doc

    def extract_metadata(self, soup_or_content) -> dict[str, Any]:
        """Extract HTML metadata."""
        metadata = {}

        if OPTIONAL_IMPORTS and hasattr(soup_or_content, "find"):
            soup = soup_or_content

            # Extract meta tags
            meta_tags = soup.find_all("meta")
            for tag in meta_tags:
                if tag.get("name"):
                    metadata[tag["name"]] = tag.get("content", "")
                elif tag.get("property"):
                    metadata[tag["property"]] = tag.get("content", "")

            # Extract title
            title_tag = soup.find("title")
            if title_tag:
                metadata["title"] = title_tag.get_text().strip()

        return metadata

    def _extract_title(self, content: str) -> str:
        """Extract title from HTML content."""
        if OPTIONAL_IMPORTS:
            soup = BeautifulSoup(content, "html.parser")
            title_tag = soup.find("title")
            if title_tag:
                return title_tag.get_text().strip()

            h1_tag = soup.find("h1")
            if h1_tag:
                return h1_tag.get_text().strip()

        return "Untitled"

    def _extract_text(self, content: str) -> str:
        """Extract plain text from HTML content."""
        if OPTIONAL_IMPORTS:
            soup = BeautifulSoup(content, "html.parser")
            return soup.get_text()
        return content

    def _extract_headers(self, soup) -> list[str]:
        """Extract headers from HTML content."""
        headers = []
        if OPTIONAL_IMPORTS:
            for i in range(1, 7):
                header_tags = soup.find_all(f"h{i}")
                for tag in header_tags:
                    headers.append(tag.get_text().strip())
        return headers

    def _extract_links(self, soup) -> list[str]:
        """Extract links from HTML content."""
        links = []
        if OPTIONAL_IMPORTS:
            link_tags = soup.find_all("a", href=True)
            for tag in link_tags:
                links.append(tag["href"])
        return links

    def _extract_images(self, soup) -> list[str]:
        """Extract images from HTML content."""
        images = []
        if OPTIONAL_IMPORTS:
            img_tags = soup.find_all("img", src=True)
            for tag in img_tags:
                images.append(tag["src"])
        return images

    def _extract_code_blocks(self, soup) -> list[dict[str, str]]:
        """Extract code blocks from HTML content."""
        code_blocks = []
        if OPTIONAL_IMPORTS:
            code_tags = soup.find_all(["code", "pre"])
            for tag in code_tags:
                language = tag.get("class", ["text"])[0] if tag.get("class") else "text"
                code_blocks.append({"language": language, "code": tag.get_text().strip()})
        return code_blocks


class CodeDocumentationExtractor:
    """Extracts documentation from code files."""

    def __init__(self):
        self.java_parser = JavaDocExtractor()
        self.python_parser = PythonDocExtractor()
        self.js_parser = JSDocExtractor()
        self.ts_parser = TSDocExtractor()

    def extract_from_file(self, file_path: str) -> CodeFile:
        """Extract documentation from a code file."""
        file_extension = Path(file_path).suffix.lower()

        try:
            with open(file_path, encoding="utf-8") as f:
                content = f.read()
        except UnicodeDecodeError:
            try:
                with open(file_path, encoding="latin-1") as f:
                    content = f.read()
            except Exception as e:
                logger.warning(f"Could not read file: {file_path}, error: {e}")
                return CodeFile(path=file_path, language="unknown")

        if file_extension == ".java":
            return self.java_parser.extract(content, file_path)
        elif file_extension == ".py":
            return self.python_parser.extract(content, file_path)
        elif file_extension == ".js":
            return self.js_parser.extract(content, file_path)
        elif file_extension == ".ts":
            return self.ts_parser.extract(content, file_path)
        else:
            return self._extract_basic(content, file_path)

    def _extract_basic(self, content: str, file_path: str) -> CodeFile:
        """Basic extraction for unsupported file types."""
        file_extension = Path(file_path).suffix.lower()
        language = file_extension[1:] if file_extension else "unknown"

        code_file = CodeFile(path=file_path, language=language)

        # Extract basic comments
        comment_patterns = [
            r"//\s*(.*?)$",  # Single-line comments
            r"/\*\s*(.*?)\s*\*/",  # Multi-line comments
            r"#\s*(.*?)$",  # Shell/Python comments
            r"--\s*(.*?)$",  # SQL comments
        ]

        for pattern in comment_patterns:
            matches = re.findall(pattern, content, re.MULTILINE | re.DOTALL)
            for match in matches:
                code_file.comments.append({"type": "comment", "content": match.strip(), "line": 0})

        return code_file


class JavaDocExtractor:
    """Extracts Javadoc and documentation from Java files."""

    def extract(self, content: str, file_path: str) -> CodeFile:
        """Extract documentation from Java code."""
        code_file = CodeFile(path=file_path, language="java")

        if OPTIONAL_IMPORTS:
            try:
                tree = javalang.parse.parse(content)

                # Extract classes
                for _path, node in tree.filter(javalang.tree.ClassDeclaration):
                    class_info = {
                        "name": node.name,
                        "type": "class",
                        "modifiers": node.modifiers,
                        "documentation": node.documentation,
                        "line": getattr(node, "position", {}).get("line", 0),
                    }
                    code_file.classes.append(class_info)

                # Extract methods
                for _path, node in tree.filter(javalang.tree.MethodDeclaration):
                    method_info = {
                        "name": node.name,
                        "type": "method",
                        "modifiers": node.modifiers,
                        "documentation": node.documentation,
                        "parameters": [param.name for param in node.parameters] if node.parameters else [],
                        "return_type": str(node.return_type) if node.return_type else "void",
                        "line": getattr(node, "position", {}).get("line", 0),
                    }
                    code_file.functions.append(method_info)

                # Extract constructors
                for _path, node in tree.filter(javalang.tree.ConstructorDeclaration):
                    constructor_info = {
                        "name": node.name,
                        "type": "constructor",
                        "modifiers": node.modifiers,
                        "documentation": node.documentation,
                        "parameters": [param.name for param in node.parameters] if node.parameters else [],
                        "line": getattr(node, "position", {}).get("line", 0),
                    }
                    code_file.functions.append(constructor_info)

            except Exception as e:
                logger.warning(f"Error parsing Java file {file_path}: {e}")

        # Extract Javadoc comments
        javadoc_pattern = r"/\*\*(.*?)\*/"
        matches = re.findall(javadoc_pattern, content, re.DOTALL)
        for match in matches:
            code_file.javadoc_comments.append({"content": match.strip(), "tags": self._extract_javadoc_tags(match)})

        # Extract Spring/REST annotations
        annotation_patterns = [
            r"@RestController",
            r"@RequestMapping\([^)]+\)",
            r"@GetMapping\([^)]+\)",
            r"@PostMapping\([^)]+\)",
            r"@PutMapping\([^)]+\)",
            r"@DeleteMapping\([^)]+\)",
            r"@PathVariable",
            r"@RequestBody",
            r"@RequestParam",
            r"@ApiOperation\([^)]+\)",
            r"@ApiParam\([^)]+\)",
        ]

        for pattern in annotation_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                code_file.annotations.append(
                    {
                        "type": "annotation",
                        "content": match,
                        "category": "rest_api" if any(x in match for x in ["Mapping", "Controller"]) else "other",
                    }
                )

        # Calculate documentation coverage
        total_items = len(code_file.classes) + len(code_file.functions)
        documented_items = sum(1 for item in code_file.classes + code_file.functions if item.get("documentation"))
        code_file.documentation_coverage = (documented_items / total_items * 100) if total_items > 0 else 0

        return code_file

    def _extract_javadoc_tags(self, javadoc_content: str) -> dict[str, list[str]]:
        """Extract Javadoc tags from content."""
        tags = defaultdict(list)

        tag_pattern = r"@(\w+)\s+([^@]*?)(?=@|\Z)"
        matches = re.findall(tag_pattern, javadoc_content, re.DOTALL)

        for tag, content in matches:
            tags[tag].append(content.strip())

        return dict(tags)


class PythonDocExtractor:
    """Extracts docstrings and documentation from Python files."""

    def extract(self, content: str, file_path: str) -> CodeFile:
        """Extract documentation from Python code."""
        code_file = CodeFile(path=file_path, language="python")

        try:
            tree = ast.parse(content)

            # Extract classes
            for node in ast.walk(tree):
                if isinstance(node, ast.ClassDef):
                    class_info = {
                        "name": node.name,
                        "type": "class",
                        "docstring": ast.get_docstring(node),
                        "line": node.lineno,
                        "methods": [],
                    }

                    # Extract methods
                    for child in node.body:
                        if isinstance(child, ast.FunctionDef):
                            method_info = {
                                "name": child.name,
                                "type": "method",
                                "docstring": ast.get_docstring(child),
                                "line": child.lineno,
                                "args": [arg.arg for arg in child.args.args],
                                "decorators": [self._get_decorator_name(dec) for dec in child.decorator_list],
                            }
                            class_info["methods"].append(method_info)

                    code_file.classes.append(class_info)

                elif isinstance(node, ast.FunctionDef):
                    function_info = {
                        "name": node.name,
                        "type": "function",
                        "docstring": ast.get_docstring(node),
                        "line": node.lineno,
                        "args": [arg.arg for arg in node.args.args],
                        "decorators": [self._get_decorator_name(dec) for dec in node.decorator_list],
                    }
                    code_file.functions.append(function_info)

        except SyntaxError as e:
            logger.warning(f"Syntax error in Python file {file_path}: {e}")
        except Exception as e:
            logger.warning(f"Error parsing Python file {file_path}: {e}")

        # Extract docstrings
        docstring_pattern = r'"""(.*?)"""'
        matches = re.findall(docstring_pattern, content, re.DOTALL)
        for match in matches:
            code_file.docstrings.append({"content": match.strip(), "type": "docstring"})

        # Extract Flask/FastAPI route decorators
        route_patterns = [
            r"@app\.route\([^)]+\)",
            r"@bp\.route\([^)]+\)",
            r"@router\.(get|post|put|delete|patch)\([^)]+\)",
            r"@api\.route\([^)]+\)",
        ]

        for pattern in route_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                code_file.api_endpoints.append({"decorator": match, "type": "route"})

        # Calculate documentation coverage
        total_items = len(code_file.classes) + len(code_file.functions)
        documented_items = 0

        for item in code_file.classes + code_file.functions:
            if item.get("docstring"):
                documented_items += 1

        code_file.documentation_coverage = (documented_items / total_items * 100) if total_items > 0 else 0

        return code_file

    def _get_decorator_name(self, decorator):
        """Get decorator name from AST node."""
        if isinstance(decorator, ast.Name):
            return decorator.id
        elif isinstance(decorator, ast.Attribute):
            return f"{decorator.value.id}.{decorator.attr}"
        elif isinstance(decorator, ast.Call):
            return self._get_decorator_name(decorator.func)
        else:
            return str(decorator)


class JSDocExtractor:
    """Extracts JSDoc and documentation from JavaScript files."""

    def extract(self, content: str, file_path: str) -> CodeFile:
        """Extract documentation from JavaScript code."""
        code_file = CodeFile(path=file_path, language="javascript")

        # Extract JSDoc comments
        jsdoc_pattern = r"/\*\*(.*?)\*/"
        matches = re.findall(jsdoc_pattern, content, re.DOTALL)
        for match in matches:
            code_file.comments.append(
                {"type": "jsdoc", "content": match.strip(), "tags": self._extract_jsdoc_tags(match)}
            )

        # Extract functions
        function_patterns = [
            r"function\s+(\w+)\s*\([^)]*\)",
            r"const\s+(\w+)\s*=\s*function\s*\([^)]*\)",
            r"const\s+(\w+)\s*=\s*\([^)]*\)\s*=>",
            r"(\w+)\s*:\s*function\s*\([^)]*\)",
            r"(\w+)\s*\([^)]*\)\s*\{",  # Method definition
        ]

        for pattern in function_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                code_file.functions.append({"name": match, "type": "function", "line": 0})

        # Extract classes
        class_pattern = r"class\s+(\w+)"
        matches = re.findall(class_pattern, content)
        for match in matches:
            code_file.classes.append({"name": match, "type": "class", "line": 0})

        # Extract API endpoints (Express.js style)
        endpoint_patterns = [
            r"app\.(get|post|put|delete|patch)\([^)]+\)",
            r"router\.(get|post|put|delete|patch)\([^)]+\)",
            r"\.route\([^)]+\)\.(get|post|put|delete|patch)\([^)]+\)",
        ]

        for pattern in endpoint_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                code_file.api_endpoints.append(
                    {"method": match if isinstance(match, str) else match[0], "type": "endpoint"}
                )

        return code_file

    def _extract_jsdoc_tags(self, jsdoc_content: str) -> dict[str, list[str]]:
        """Extract JSDoc tags from content."""
        tags = defaultdict(list)

        tag_pattern = r"@(\w+)\s+([^@]*?)(?=@|\Z)"
        matches = re.findall(tag_pattern, jsdoc_content, re.DOTALL)

        for tag, content in matches:
            tags[tag].append(content.strip())

        return dict(tags)


class TSDocExtractor:
    """Extracts TSDoc and documentation from TypeScript files."""

    def extract(self, content: str, file_path: str) -> CodeFile:
        """Extract documentation from TypeScript code."""
        code_file = CodeFile(path=file_path, language="typescript")

        # Extract TSDoc comments
        tsdoc_pattern = r"/\*\*(.*?)\*/"
        matches = re.findall(tsdoc_pattern, content, re.DOTALL)
        for match in matches:
            code_file.comments.append(
                {"type": "tsdoc", "content": match.strip(), "tags": self._extract_tsdoc_tags(match)}
            )

        # Extract interfaces
        interface_pattern = r"interface\s+(\w+)"
        matches = re.findall(interface_pattern, content)
        for match in matches:
            code_file.classes.append({"name": match, "type": "interface", "line": 0})

        # Extract types
        type_pattern = r"type\s+(\w+)\s*="
        matches = re.findall(type_pattern, content)
        for match in matches:
            code_file.classes.append({"name": match, "type": "type", "line": 0})

        # Extract functions (similar to JavaScript but with type annotations)
        function_patterns = [
            r"function\s+(\w+)\s*\([^)]*\)\s*:\s*\w+",
            r"const\s+(\w+)\s*=\s*\([^)]*\)\s*:\s*\w+\s*=>",
            r"(\w+)\s*\([^)]*\)\s*:\s*\w+\s*\{",
        ]

        for pattern in function_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                code_file.functions.append({"name": match, "type": "function", "line": 0})

        return code_file

    def _extract_tsdoc_tags(self, tsdoc_content: str) -> dict[str, list[str]]:
        """Extract TSDoc tags from content."""
        tags = defaultdict(list)

        tag_pattern = r"@(\w+)\s+([^@]*?)(?=@|\Z)"
        matches = re.findall(tag_pattern, tsdoc_content, re.DOTALL)

        for tag, content in matches:
            tags[tag].append(content.strip())

        return dict(tags)


class DocumentationQualityAnalyzer:
    """Analyzes documentation quality and provides scores."""

    def __init__(self):
        self.lemmatizer = None
        self.stop_words = set()
        if OPTIONAL_IMPORTS:
            try:
                import nltk

                nltk.download("punkt", quiet=True)
                nltk.download("stopwords", quiet=True)
                nltk.download("wordnet", quiet=True)
                self.lemmatizer = WordNetLemmatizer()
                self.stop_words = set(stopwords.words("english"))
            except Exception:
                # NLTK dependencies are optional
                pass

    def analyze_document(self, doc: DocumentationFile) -> DocumentationFile:
        """Analyze a single document and update its quality scores."""
        doc.readability_score = self._calculate_readability(doc.content)
        doc.completeness_score = self._calculate_completeness(doc)
        doc.freshness_score = self._calculate_freshness(doc)
        doc.quality_score = self._calculate_overall_quality(doc)

        return doc

    def _calculate_readability(self, content: str) -> float:
        """Calculate readability score using various metrics."""
        if not content.strip():
            return 0.0

        # Basic readability metrics
        try:
            if OPTIONAL_IMPORTS:
                sentences = len(sent_tokenize(content))
                words = len(word_tokenize(content))
            else:
                sentences = len([s for s in content.split(".") if s.strip()])
                words = len(content.split())
        except Exception:
            # Fallback to simple counting if NLTK is not available
            sentences = len([s for s in content.split(".") if s.strip()])
            words = len(content.split())

        if sentences == 0:
            return 0.0

        # Average sentence length
        avg_sentence_length = words / sentences

        # Simple readability score (inverse of average sentence length, normalized)
        readability = max(0, 100 - (avg_sentence_length * 2))

        # Adjust for technical terms (lower score for many technical terms)
        technical_indicators = ["API", "HTTP", "JSON", "XML", "SQL", "REST", "CRUD"]
        technical_count = sum(content.upper().count(term) for term in technical_indicators)
        technical_penalty = min(20, technical_count * 2)

        return max(0, min(100, readability - technical_penalty))

    def _calculate_completeness(self, doc: DocumentationFile) -> float:
        """Calculate completeness score based on document structure."""
        score = 0.0

        # Check for title
        if doc.title and doc.title != "Untitled":
            score += 10

        # Check for headers (good structure)
        if doc.headers:
            score += 20
            if len(doc.headers) >= 3:
                score += 10

        # Check for examples
        if doc.examples:
            score += 15

        # Check for code blocks
        if doc.code_blocks:
            score += 10

        # Check for links (references)
        if doc.links:
            score += 10

        # Check for images/diagrams
        if doc.images:
            score += 10

        # Check for metadata
        if doc.metadata:
            score += 10

        # Check for content length (not too short, not too long)
        if 100 <= doc.word_count <= 5000:
            score += 15
        elif doc.word_count > 5000:
            score += 10
        elif doc.word_count > 50:
            score += 5

        return min(100, score)

    def _calculate_freshness(self, doc: DocumentationFile) -> float:
        """Calculate freshness score based on last modification time."""
        if not hasattr(doc, "last_modified") or not doc.last_modified:
            return 50.0  # Default score if no modification time

        now = datetime.datetime.now()
        days_old = (now - doc.last_modified).days

        if days_old < 30:
            return 100.0
        elif days_old < 90:
            return 80.0
        elif days_old < 180:
            return 60.0
        elif days_old < 365:
            return 40.0
        else:
            return 20.0

    def _calculate_overall_quality(self, doc: DocumentationFile) -> float:
        """Calculate overall quality score."""
        weights = {"readability": 0.25, "completeness": 0.40, "freshness": 0.35}

        return (
            doc.readability_score * weights["readability"]
            + doc.completeness_score * weights["completeness"]
            + doc.freshness_score * weights["freshness"]
        )


class DocumentationCoverageAnalyzer:
    """Analyzes documentation coverage across the project."""

    def __init__(self):
        self.code_extractor = CodeDocumentationExtractor()

    def analyze_coverage(self, project_path: str) -> dict[str, Any]:
        """Analyze documentation coverage for the entire project."""
        coverage_data = {
            "total_files": 0,
            "documented_files": 0,
            "coverage_by_type": {},
            "missing_docs": [],
            "api_coverage": {},
            "class_coverage": {},
            "function_coverage": {},
            "overall_coverage": 0.0,
        }

        # Find all code files
        code_files = []
        for root, dirs, files in os.walk(project_path):
            # Skip common directories
            dirs[:] = [d for d in dirs if d not in {".git", "node_modules", "target", "build", ".gradle"}]

            for file in files:
                if file.endswith((".java", ".py", ".js", ".ts", ".jsx", ".tsx")):
                    code_files.append(os.path.join(root, file))

        coverage_data["total_files"] = len(code_files)

        # Analyze each code file
        for file_path in code_files:
            try:
                code_file = self.code_extractor.extract_from_file(file_path)

                # Check if file has documentation
                has_docs = (
                    code_file.documentation_coverage > 0 or len(code_file.comments) > 0 or len(code_file.docstrings) > 0
                )

                if has_docs:
                    coverage_data["documented_files"] += 1
                else:
                    coverage_data["missing_docs"].append(file_path)

                # Coverage by file type
                file_type = code_file.language
                if file_type not in coverage_data["coverage_by_type"]:
                    coverage_data["coverage_by_type"][file_type] = {"total": 0, "documented": 0, "coverage": 0.0}

                coverage_data["coverage_by_type"][file_type]["total"] += 1
                if has_docs:
                    coverage_data["coverage_by_type"][file_type]["documented"] += 1

                # API coverage
                if code_file.api_endpoints:
                    for endpoint in code_file.api_endpoints:
                        if file_path not in coverage_data["api_coverage"]:
                            coverage_data["api_coverage"][file_path] = []
                        coverage_data["api_coverage"][file_path].append(endpoint)

                # Class coverage
                for class_info in code_file.classes:
                    key = f"{file_path}:{class_info['name']}"
                    coverage_data["class_coverage"][key] = {
                        "documented": bool(class_info.get("documentation") or class_info.get("docstring")),
                        "class_info": class_info,
                    }

                # Function coverage
                for func_info in code_file.functions:
                    key = f"{file_path}:{func_info['name']}"
                    coverage_data["function_coverage"][key] = {
                        "documented": bool(func_info.get("documentation") or func_info.get("docstring")),
                        "function_info": func_info,
                    }

            except Exception as e:
                logger.warning(f"Error analyzing {file_path}: {e}")

        # Calculate coverage percentages
        for _file_type, data in coverage_data["coverage_by_type"].items():
            if data["total"] > 0:
                data["coverage"] = (data["documented"] / data["total"]) * 100

        # Calculate overall coverage
        if coverage_data["total_files"] > 0:
            coverage_data["overall_coverage"] = (coverage_data["documented_files"] / coverage_data["total_files"]) * 100

        return coverage_data


class DocumentationSearchIndex:
    """Creates a searchable index of documentation with semantic understanding."""

    def __init__(self):
        self.vectorizer = None
        self.sentence_model = None
        self.documents = []
        self.index = {}

        if OPTIONAL_IMPORTS:
            try:
                self.vectorizer = TfidfVectorizer(stop_words="english", max_features=5000)
                # For semantic search, we'd use a sentence transformer
                # self.sentence_model = SentenceTransformer('all-MiniLM-L6-v2')
            except Exception:
                # Optional imports may not be available
                pass

    def build_index(self, docs: list[DocumentationFile]) -> None:
        """Build search index from documentation files."""
        self.documents = docs

        # Build text corpus
        texts = []
        for doc in docs:
            # Combine title, headers, and content for indexing
            text = f"{doc.title} {' '.join(doc.headers)} {doc.content}"
            texts.append(text)

        # Build TF-IDF index
        if self.vectorizer and texts:
            try:
                tfidf_matrix = self.vectorizer.fit_transform(texts)

                # Build keyword index
                feature_names = self.vectorizer.get_feature_names_out()
                for i, doc in enumerate(docs):
                    doc_vector = tfidf_matrix[i]
                    # Get top keywords for this document
                    top_indices = doc_vector.toarray()[0].argsort()[-10:][::-1]
                    keywords = [feature_names[idx] for idx in top_indices if doc_vector[0, idx] > 0]

                    self.index[doc.path] = {"keywords": keywords, "vector": doc_vector, "content": texts[i]}
            except Exception as e:
                logger.warning(f"Error building search index: {e}")

    def search(self, query: str, top_k: int = 10) -> list[tuple[DocumentationFile, float]]:
        """Search for documents matching the query."""
        if not self.vectorizer or not self.index:
            return []

        try:
            # Transform query
            query_vector = self.vectorizer.transform([query])

            # Calculate similarities
            results = []
            for doc in self.documents:
                if doc.path in self.index:
                    doc_vector = self.index[doc.path]["vector"]
                    similarity = cosine_similarity(query_vector, doc_vector)[0][0]
                    results.append((doc, similarity))

            # Sort by similarity
            results.sort(key=lambda x: x[1], reverse=True)
            return results[:top_k]

        except Exception as e:
            logger.warning(f"Error searching: {e}")
            return []

    def get_related_documents(self, doc_path: str, top_k: int = 5) -> list[tuple[DocumentationFile, float]]:
        """Find documents related to the given document."""
        if not self.vectorizer or not self.index or doc_path not in self.index:
            return []

        try:
            doc_vector = self.index[doc_path]["vector"]

            # Calculate similarities with other documents
            results = []
            for other_doc in self.documents:
                if other_doc.path != doc_path and other_doc.path in self.index:
                    other_vector = self.index[other_doc.path]["vector"]
                    similarity = cosine_similarity(doc_vector, other_vector)[0][0]
                    results.append((other_doc, similarity))

            # Sort by similarity
            results.sort(key=lambda x: x[1], reverse=True)
            return results[:top_k]

        except Exception as e:
            logger.warning(f"Error finding related documents: {e}")
            return []


class OutdatedDocumentationDetector:
    """Detects outdated documentation by comparing with code changes."""

    def __init__(self):
        self.code_extractor = CodeDocumentationExtractor()

    def detect_outdated_docs(self, project_path: str) -> list[DocumentationIssue]:
        """Detect potentially outdated documentation."""
        issues = []

        # Get git history for file modifications
        try:
            # Get recently modified code files
            result = subprocess.run(
                ["git", "log", "--name-only", "--pretty=format:", "--since=30 days ago"],
                cwd=project_path,
                capture_output=True,
                text=True,
                check=False,
            )

            if result.returncode == 0:
                modified_files = [f for f in result.stdout.split("\n") if f.strip()]
                code_files = [f for f in modified_files if f.endswith((".java", ".py", ".js", ".ts"))]

                # Check if corresponding documentation exists and is recent
                for code_file in code_files:
                    doc_candidates = self._find_related_docs(code_file, project_path)

                    for doc_path in doc_candidates:
                        if os.path.exists(doc_path):
                            # Check if documentation is older than code
                            code_mtime = os.path.getmtime(os.path.join(project_path, code_file))
                            doc_mtime = os.path.getmtime(doc_path)

                            if doc_mtime < code_mtime:
                                issues.append(
                                    DocumentationIssue(
                                        severity="warning",
                                        type="outdated",
                                        message="Documentation may be outdated (code modified more recently)",
                                        file_path=doc_path,
                                        suggestion=f"Review and update documentation for {code_file}",
                                    )
                                )

        except subprocess.CalledProcessError:
            logger.warning("Could not access git history")

        # Check for broken links
        issues.extend(self._check_broken_links(project_path))

        # Check for missing API documentation
        issues.extend(self._check_missing_api_docs(project_path))

        return issues

    def _find_related_docs(self, code_file: str, project_path: str) -> list[str]:
        """Find documentation files related to a code file."""
        base_name = os.path.splitext(os.path.basename(code_file))[0]
        candidates = []

        # Look for documentation files with similar names
        for root, _dirs, files in os.walk(os.path.join(project_path, "docs")):
            for file in files:
                if file.endswith((".md", ".rst", ".adoc")) and base_name.lower() in file.lower():
                    candidates.append(os.path.join(root, file))

        return candidates

    def _check_broken_links(self, project_path: str) -> list[DocumentationIssue]:
        """Check for broken links in documentation."""
        issues = []

        # Find all documentation files
        for root, _dirs, files in os.walk(project_path):
            for file in files:
                if file.endswith((".md", ".rst", ".adoc")):
                    file_path = os.path.join(root, file)

                    try:
                        with open(file_path, encoding="utf-8") as f:
                            content = f.read()

                        # Check internal links
                        link_pattern = r"\[([^\]]+)\]\(([^)]+)\)"
                        links = re.findall(link_pattern, content)

                        for _text, url in links:
                            # Check if it's a local file link
                            if not url.startswith(("http://", "https://", "mailto:")):
                                # Resolve relative path
                                if url.startswith("/"):
                                    target_path = os.path.join(project_path, url[1:])
                                else:
                                    target_path = os.path.join(os.path.dirname(file_path), url)

                                # Remove anchor
                                if "#" in target_path:
                                    target_path = target_path.split("#")[0]

                                if not os.path.exists(target_path):
                                    issues.append(
                                        DocumentationIssue(
                                            severity="error",
                                            type="broken_link",
                                            message=f"Broken link to {url}",
                                            file_path=file_path,
                                            suggestion=f"Fix or remove broken link to {url}",
                                        )
                                    )

                    except Exception as e:
                        logger.warning(f"Error checking links in {file_path}: {e}")

        return issues

    def _check_missing_api_docs(self, project_path: str) -> list[DocumentationIssue]:
        """Check for missing API documentation."""
        issues = []

        # Find all API endpoints
        api_endpoints = []
        for root, _dirs, files in os.walk(project_path):
            for file in files:
                if file.endswith((".java", ".py", ".js", ".ts")):
                    file_path = os.path.join(root, file)

                    try:
                        code_file = self.code_extractor.extract_from_file(file_path)
                        if code_file.api_endpoints:
                            api_endpoints.extend([(file_path, endpoint) for endpoint in code_file.api_endpoints])
                    except Exception as e:
                        logger.warning(f"Error analyzing {file_path}: {e}")

        # Check if API documentation exists
        api_doc_files = []
        for root, _dirs, files in os.walk(os.path.join(project_path, "docs")):
            for file in files:
                if "api" in file.lower() and file.endswith((".md", ".rst", ".adoc")):
                    api_doc_files.append(os.path.join(root, file))

        if api_endpoints and not api_doc_files:
            issues.append(
                DocumentationIssue(
                    severity="warning",
                    type="missing_api_docs",
                    message=f"Found {len(api_endpoints)} API endpoints but no API documentation",
                    file_path=project_path,
                    suggestion="Create API documentation files",
                )
            )

        return issues


class DocumentationRecommendationEngine:
    """Generates recommendations for improving documentation."""

    def __init__(self):
        self.quality_analyzer = DocumentationQualityAnalyzer()
        self.coverage_analyzer = DocumentationCoverageAnalyzer()
        self.outdated_detector = OutdatedDocumentationDetector()

    def generate_recommendations(self, project_path: str) -> dict[str, Any]:
        """Generate comprehensive documentation recommendations."""
        recommendations = {
            "high_priority": [],
            "medium_priority": [],
            "low_priority": [],
            "quick_wins": [],
            "long_term": [],
            "templates": [],
            "tools": [],
        }

        # Analyze coverage
        coverage_data = self.coverage_analyzer.analyze_coverage(project_path)

        # High priority recommendations
        if coverage_data["overall_coverage"] < 50:
            recommendations["high_priority"].append(
                {
                    "type": "coverage",
                    "title": "Improve Documentation Coverage",
                    "description": f"Only {coverage_data['overall_coverage']:.1f}% of code files have documentation",
                    "action": "Add documentation to critical files",
                    "files": coverage_data["missing_docs"][:10],
                }
            )

        # Check for missing API documentation
        if coverage_data["api_coverage"]:
            api_files = list(coverage_data["api_coverage"].keys())
            recommendations["high_priority"].append(
                {
                    "type": "api_docs",
                    "title": "Create API Documentation",
                    "description": f"Found API endpoints in {len(api_files)} files",
                    "action": "Create comprehensive API documentation",
                    "files": api_files,
                }
            )

        # Medium priority recommendations
        for file_type, data in coverage_data["coverage_by_type"].items():
            if data["coverage"] < 70:
                recommendations["medium_priority"].append(
                    {
                        "type": "file_type_coverage",
                        "title": f"Improve {file_type.title()} Documentation",
                        "description": f"Only {data['coverage']:.1f}% of {file_type} files are documented",
                        "action": f"Add documentation to {file_type} files",
                        "count": data["total"] - data["documented"],
                    }
                )

        # Check for outdated documentation
        outdated_issues = self.outdated_detector.detect_outdated_docs(project_path)
        if outdated_issues:
            recommendations["medium_priority"].append(
                {
                    "type": "outdated",
                    "title": "Update Outdated Documentation",
                    "description": f"Found {len(outdated_issues)} potentially outdated documentation issues",
                    "action": "Review and update outdated documentation",
                    "issues": outdated_issues,
                }
            )

        # Quick wins
        recommendations["quick_wins"].extend(
            [
                {
                    "type": "readme",
                    "title": "Improve README",
                    "description": "Add badges, setup instructions, and contribution guidelines",
                    "action": "Update README.md with project status and quick start guide",
                    "effort": "low",
                },
                {
                    "type": "code_comments",
                    "title": "Add Code Comments",
                    "description": "Add inline comments to complex code sections",
                    "action": "Review and add comments to complex functions and classes",
                    "effort": "low",
                },
            ]
        )

        # Long-term recommendations
        recommendations["long_term"].extend(
            [
                {
                    "type": "documentation_site",
                    "title": "Create Documentation Site",
                    "description": "Set up a documentation website with GitBook, Docusaurus, or similar",
                    "action": "Implement documentation site with search and navigation",
                    "effort": "high",
                },
                {
                    "type": "api_specs",
                    "title": "OpenAPI/Swagger Specifications",
                    "description": "Create formal API specifications",
                    "action": "Add OpenAPI/Swagger documentation for all REST endpoints",
                    "effort": "medium",
                },
            ]
        )

        # Template recommendations
        recommendations["templates"].extend(
            [
                {
                    "type": "template",
                    "name": "API Endpoint Template",
                    "description": "Template for documenting REST API endpoints",
                    "content": self._get_api_template(),
                },
                {
                    "type": "template",
                    "name": "Class Documentation Template",
                    "description": "Template for documenting classes and interfaces",
                    "content": self._get_class_template(),
                },
            ]
        )

        # Tool recommendations
        recommendations["tools"].extend(
            [
                {
                    "name": "Javadoc",
                    "description": "Generate HTML documentation from Java code comments",
                    "usage": "mvn javadoc:javadoc",
                    "applicable": "java" in coverage_data["coverage_by_type"],
                },
                {
                    "name": "Sphinx",
                    "description": "Generate documentation from Python docstrings",
                    "usage": "sphinx-quickstart && sphinx-build -b html source build",
                    "applicable": "python" in coverage_data["coverage_by_type"],
                },
            ]
        )

        return recommendations

    def _get_api_template(self) -> str:
        """Get API documentation template."""
        return """
# API Endpoint: {endpoint_name}

## Overview
Brief description of what this endpoint does.

## Request
- **Method:** {method}
- **URL:** {url}
- **Headers:**
  - Content-Type: application/json
  - Authorization: Bearer {token}

## Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| param1    | string | Yes | Description of param1 |
| param2    | integer | No | Description of param2 |

## Request Body
```json
{
  "example": "request body"
}
```

## Response
### Success (200 OK)
```json
{
  "success": true,
  "data": {}
}
```

### Error (400 Bad Request)
```json
{
  "error": "Error message",
  "details": {}
}
```

## Examples
### cURL
```bash
curl -X {method} \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer TOKEN" \\
  -d '{"example": "data"}' \\
  {url}
```

### JavaScript
```javascript
fetch('{url}', {
  method: '{method}',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer TOKEN'
  },
  body: JSON.stringify({example: 'data'})
})
```
"""

    def _get_class_template(self) -> str:
        """Get class documentation template."""
        return """
# Class: {class_name}

## Overview
Brief description of what this class does and its purpose.

## Constructor
```java
public {class_name}({parameters})
```

## Properties
| Property | Type | Description |
|----------|------|-------------|
| property1 | String | Description of property1 |
| property2 | int | Description of property2 |

## Methods

### {method_name}
```java
public {return_type} {method_name}({parameters})
```

**Description:** What this method does.

**Parameters:**
- `param1` (String): Description of param1
- `param2` (int): Description of param2

**Returns:** Description of return value

**Example:**
```java
{class_name} instance = new {class_name}();
{return_type} result = instance.{method_name}(param1, param2);
```

## Usage Examples
```java
// Example usage
{class_name} example = new {class_name}();
// ... usage code
```

## See Also
- Related classes
- Related documentation
"""


class DocumentationAnalysisSystem:
    """Main system that orchestrates all documentation analysis components."""

    def __init__(self):
        self.parsers = {
            "markdown": MarkdownParser(),
            "asciidoc": AsciiDocParser(),
            "rst": RestructuredTextParser(),
            "html": HtmlParser(),
        }
        self.code_extractor = CodeDocumentationExtractor()
        self.quality_analyzer = DocumentationQualityAnalyzer()
        self.coverage_analyzer = DocumentationCoverageAnalyzer()
        self.search_index = DocumentationSearchIndex()
        self.outdated_detector = OutdatedDocumentationDetector()
        self.recommendation_engine = DocumentationRecommendationEngine()

    def analyze_project(self, project_path: str) -> dict[str, Any]:
        """Perform comprehensive documentation analysis of a project."""
        logger.info(f"Starting documentation analysis for {project_path}")

        analysis_results = {
            "project_path": project_path,
            "timestamp": datetime.datetime.now().isoformat(),
            "documentation_files": [],
            "code_files": [],
            "metrics": DocumentationMetrics(),
            "issues": [],
            "recommendations": {},
            "search_index_built": False,
        }

        # Find and parse documentation files
        docs = self._find_and_parse_docs(project_path)
        analysis_results["documentation_files"] = [asdict(doc) for doc in docs]

        # Analyze code files
        code_files = self._analyze_code_files(project_path)
        analysis_results["code_files"] = [asdict(cf) for cf in code_files]

        # Calculate metrics
        analysis_results["metrics"] = self._calculate_metrics(docs, code_files)

        # Detect issues
        analysis_results["issues"] = self._detect_issues(project_path, docs)

        # Build search index
        try:
            self.search_index.build_index(docs)
            analysis_results["search_index_built"] = True
        except Exception as e:
            logger.warning(f"Could not build search index: {e}")

        # Generate recommendations
        analysis_results["recommendations"] = self.recommendation_engine.generate_recommendations(project_path)

        logger.info("Documentation analysis completed")
        return analysis_results

    def _find_and_parse_docs(self, project_path: str) -> list[DocumentationFile]:
        """Find and parse all documentation files."""
        docs = []

        # Common documentation file patterns

        for root, dirs, files in os.walk(project_path):
            # Skip certain directories
            dirs[:] = [d for d in dirs if d not in {".git", "node_modules", "target", "build"}]

            for file in files:
                file_path = os.path.join(root, file)
                file_extension = Path(file).suffix.lower()

                # Determine parser
                parser = None
                if file_extension == ".md":
                    parser = self.parsers["markdown"]
                elif file_extension == ".rst":
                    parser = self.parsers["rst"]
                elif file_extension == ".adoc":
                    parser = self.parsers["asciidoc"]
                elif file_extension == ".html":
                    parser = self.parsers["html"]

                if parser:
                    try:
                        with open(file_path, encoding="utf-8") as f:
                            content = f.read()

                        doc = parser.parse(content, file_path)

                        # Get file stats
                        stat = os.stat(file_path)
                        doc.last_modified = datetime.datetime.fromtimestamp(stat.st_mtime)

                        # Analyze quality
                        doc = self.quality_analyzer.analyze_document(doc)

                        docs.append(doc)

                    except Exception as e:
                        logger.warning(f"Error parsing {file_path}: {e}")

        return docs

    def _analyze_code_files(self, project_path: str) -> list[CodeFile]:
        """Analyze code files for documentation extraction."""
        code_files = []

        for root, dirs, files in os.walk(project_path):
            # Skip certain directories
            dirs[:] = [
                d
                for d in dirs
                if d not in {".git", "node_modules", "target", "build", ".gradle", "venv", "__pycache__"}
            ]

            for file in files:
                if file.endswith((".java", ".py", ".js", ".ts", ".jsx", ".tsx")):
                    file_path = os.path.join(root, file)

                    # Check if file actually exists
                    if not os.path.exists(file_path):
                        continue

                    try:
                        code_file = self.code_extractor.extract_from_file(file_path)
                        code_files.append(code_file)
                    except Exception as e:
                        logger.warning(f"Error analyzing code file {file_path}: {e}")

        return code_files

    def _calculate_metrics(self, docs: list[DocumentationFile], code_files: list[CodeFile]) -> DocumentationMetrics:
        """Calculate comprehensive documentation metrics."""
        metrics = DocumentationMetrics()

        # Basic counts
        metrics.total_files = len(docs)
        metrics.total_words = sum(doc.word_count for doc in docs)
        metrics.total_lines = sum(doc.line_count for doc in docs)

        # Format distribution
        metrics.formats = Counter(doc.format for doc in docs)

        # Quality scores
        if docs:
            metrics.quality_score = sum(doc.quality_score for doc in docs) / len(docs)
            metrics.freshness_score = sum(doc.freshness_score for doc in docs) / len(docs)
            metrics.completeness_score = sum(doc.completeness_score for doc in docs) / len(docs)

        # Coverage calculation
        total_code_items = sum(len(cf.classes) + len(cf.functions) for cf in code_files)
        documented_code_items = sum(
            sum(1 for item in cf.classes + cf.functions if item.get("documentation") or item.get("docstring"))
            for cf in code_files
        )

        if total_code_items > 0:
            metrics.coverage_percentage = (documented_code_items / total_code_items) * 100

        # Cross-reference count
        metrics.cross_reference_count = sum(len(doc.cross_references) for doc in docs)

        return metrics

    def _detect_issues(self, project_path: str, docs: list[DocumentationFile]) -> list[DocumentationIssue]:
        """Detect various documentation issues."""
        issues = []

        # Use the outdated detector
        issues.extend(self.outdated_detector.detect_outdated_docs(project_path))

        # Check for low-quality documentation
        for doc in docs:
            if doc.quality_score < 30:
                issues.append(
                    DocumentationIssue(
                        severity="warning",
                        type="low_quality",
                        message=f"Low quality documentation (score: {doc.quality_score:.1f})",
                        file_path=doc.path,
                        suggestion="Improve structure, add examples, or update content",
                    )
                )

            if doc.word_count < 50:
                issues.append(
                    DocumentationIssue(
                        severity="info",
                        type="too_short",
                        message=f"Very short documentation ({doc.word_count} words)",
                        file_path=doc.path,
                        suggestion="Add more detailed content and examples",
                    )
                )

        return issues

    def search_documentation(self, query: str, top_k: int = 10) -> list[tuple[DocumentationFile, float]]:
        """Search documentation using the built index."""
        return self.search_index.search(query, top_k)

    def get_related_documents(self, doc_path: str, top_k: int = 5) -> list[tuple[DocumentationFile, float]]:
        """Get documents related to the specified document."""
        return self.search_index.get_related_documents(doc_path, top_k)

    def export_analysis(self, analysis_results: dict[str, Any], output_path: str) -> None:
        """Export analysis results to JSON file."""
        try:
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(analysis_results, f, indent=2, default=str)
            logger.info(f"Analysis results exported to {output_path}")
        except Exception as e:
            logger.error(f"Error exporting analysis: {e}")

    def generate_report(self, analysis_results: dict[str, Any]) -> str:
        """Generate a comprehensive markdown report."""
        report = []

        # Header
        report.append("# Documentation Analysis Report")
        report.append("")
        report.append(f"**Generated:** {analysis_results['timestamp']}")
        report.append(f"**Project:** {analysis_results['project_path']}")
        report.append("")

        # Summary
        metrics = analysis_results["metrics"]
        report.append("## Summary")
        report.append("")
        report.append(f"- **Total Documentation Files:** {metrics['total_files']}")
        report.append(f"- **Total Words:** {metrics['total_words']:,}")
        report.append(f"- **Total Lines:** {metrics['total_lines']:,}")
        report.append(f"- **Overall Quality Score:** {metrics['quality_score']:.1f}/100")
        report.append(f"- **Documentation Coverage:** {metrics['coverage_percentage']:.1f}%")
        report.append(f"- **Cross-References:** {metrics['cross_reference_count']}")
        report.append("")

        # Format distribution
        if metrics["formats"]:
            report.append("## Format Distribution")
            report.append("")
            for fmt, count in metrics["formats"].items():
                report.append(f"- **{fmt.title()}:** {count} files")
            report.append("")

        # Issues
        if analysis_results["issues"]:
            report.append("## Issues Found")
            report.append("")

            # Group issues by severity
            issues_by_severity = defaultdict(list)
            for issue in analysis_results["issues"]:
                issues_by_severity[issue["severity"]].append(issue)

            for severity in ["error", "warning", "info"]:
                if severity in issues_by_severity:
                    report.append(f"### {severity.title()} Issues")
                    report.append("")
                    for issue in issues_by_severity[severity]:
                        report.append(f"- **{issue['type'].title()}** in `{issue['file_path']}`")
                        report.append(f"  - {issue['message']}")
                        if issue.get("suggestion"):
                            report.append(f"  - *Suggestion: {issue['suggestion']}*")
                        report.append("")

        # Recommendations
        if analysis_results["recommendations"]:
            report.append("## Recommendations")
            report.append("")

            recs = analysis_results["recommendations"]

            if recs.get("high_priority"):
                report.append("### High Priority")
                report.append("")
                for rec in recs["high_priority"]:
                    report.append(f"- **{rec['title']}**")
                    report.append(f"  - {rec['description']}")
                    report.append(f"  - *Action: {rec['action']}*")
                    report.append("")

            if recs.get("quick_wins"):
                report.append("### Quick Wins")
                report.append("")
                for rec in recs["quick_wins"]:
                    report.append(f"- **{rec['title']}**")
                    report.append(f"  - {rec['description']}")
                    report.append(f"  - *Action: {rec['action']}*")
                    report.append("")

        # Tool recommendations
        if analysis_results["recommendations"].get("tools"):
            report.append("## Recommended Tools")
            report.append("")
            for tool in analysis_results["recommendations"]["tools"]:
                if tool["applicable"]:
                    report.append(f"- **{tool['name']}:** {tool['description']}")
                    report.append(f"  - Usage: `{tool['usage']}`")
                    report.append("")

        return "\n".join(report)


def main():
    """Main function for command-line usage."""
    import argparse

    parser = argparse.ArgumentParser(description="Comprehensive Documentation Analysis System")
    parser.add_argument("project_path", help="Path to the project to analyze")
    parser.add_argument("--output", "-o", help="Output file for analysis results (JSON)")
    parser.add_argument("--report", "-r", help="Output file for analysis report (Markdown)")
    parser.add_argument("--search", "-s", help="Search query for documentation")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    # Initialize the system
    system = DocumentationAnalysisSystem()

    # Analyze the project
    results = system.analyze_project(args.project_path)

    # Export results
    if args.output:
        system.export_analysis(results, args.output)

    # Generate report
    if args.report:
        report = system.generate_report(results)
        with open(args.report, "w", encoding="utf-8") as f:
            f.write(report)

    # Search functionality
    if args.search:
        search_results = system.search_documentation(args.search)
        for _doc, _score in search_results:
            pass

    # Print summary
    results["metrics"]


if __name__ == "__main__":
    main()
