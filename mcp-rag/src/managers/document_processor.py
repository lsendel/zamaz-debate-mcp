from typing import List, Optional
import structlog
from pathlib import Path
import httpx
from pypdf import PdfReader
from docx import Document as DocxDocument
import markdown
from bs4 import BeautifulSoup
import json
import csv
import io

from ..models import Document, DocumentChunk, ChunkingStrategy, IngestDocumentRequest

logger = structlog.get_logger()


class DocumentProcessor:
    """Processes various document types and chunks them"""
    
    def __init__(self):
        self.http_client = httpx.AsyncClient()
        
    async def process_document(self, request: IngestDocumentRequest) -> str:
        """Process a document from various sources"""
        content = ""
        
        if request.content:
            content = request.content
        elif request.file_path:
            content = await self._read_file(request.file_path, request.document_type)
        elif request.url:
            content = await self._fetch_url(request.url)
        else:
            raise ValueError("No content source provided")
        
        return content
    
    async def chunk_document(self, document: Document, strategy: ChunkingStrategy,
                           chunk_size: int, chunk_overlap: int) -> List[DocumentChunk]:
        """Chunk a document based on the specified strategy"""
        
        if strategy == ChunkingStrategy.FIXED_SIZE:
            return self._chunk_fixed_size(document, chunk_size, chunk_overlap)
        elif strategy == ChunkingStrategy.SENTENCE:
            return self._chunk_by_sentence(document, chunk_size)
        elif strategy == ChunkingStrategy.PARAGRAPH:
            return self._chunk_by_paragraph(document, chunk_size)
        elif strategy == ChunkingStrategy.SLIDING_WINDOW:
            return self._chunk_sliding_window(document, chunk_size, chunk_overlap)
        else:
            # Default to sliding window
            return self._chunk_sliding_window(document, chunk_size, chunk_overlap)
    
    def _chunk_sliding_window(self, document: Document, chunk_size: int,
                            chunk_overlap: int) -> List[DocumentChunk]:
        """Create overlapping chunks using a sliding window"""
        chunks = []
        text = document.content
        start = 0
        chunk_index = 0
        
        while start < len(text):
            # Find the end of the chunk
            end = start + chunk_size
            
            # Try to break at a sentence boundary
            if end < len(text):
                # Look for sentence endings
                for punct in ['. ', '! ', '? ', '\n\n']:
                    last_punct = text.rfind(punct, start, end)
                    if last_punct != -1:
                        end = last_punct + len(punct)
                        break
            
            # Create chunk
            chunk_text = text[start:end].strip()
            if chunk_text:
                chunk = DocumentChunk(
                    document_id=document.id,
                    kb_id=document.kb_id,
                    chunk_index=chunk_index,
                    content=chunk_text,
                    char_start=start,
                    char_end=end,
                    metadata={
                        "chunking_strategy": "sliding_window",
                        "chunk_size": chunk_size,
                        "overlap": chunk_overlap
                    }
                )
                chunks.append(chunk)
                chunk_index += 1
            
            # Move window
            start = end - chunk_overlap
            if start >= len(text):
                break
        
        return chunks
    
    def _chunk_fixed_size(self, document: Document, chunk_size: int,
                         chunk_overlap: int) -> List[DocumentChunk]:
        """Create fixed-size chunks"""
        chunks = []
        text = document.content
        
        for i in range(0, len(text), chunk_size - chunk_overlap):
            chunk_text = text[i:i + chunk_size]
            if chunk_text:
                chunk = DocumentChunk(
                    document_id=document.id,
                    kb_id=document.kb_id,
                    chunk_index=len(chunks),
                    content=chunk_text,
                    char_start=i,
                    char_end=i + len(chunk_text),
                    metadata={"chunking_strategy": "fixed_size"}
                )
                chunks.append(chunk)
        
        return chunks
    
    def _chunk_by_sentence(self, document: Document, max_chunk_size: int) -> List[DocumentChunk]:
        """Chunk by sentences, combining until max size"""
        # Simple sentence splitting - in production use spaCy or NLTK
        sentences = document.content.replace('! ', '!|').replace('? ', '?|').replace('. ', '.|').split('|')
        
        chunks = []
        current_chunk = []
        current_size = 0
        char_start = 0
        
        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue
            
            sentence_size = len(sentence)
            
            if current_size + sentence_size > max_chunk_size and current_chunk:
                # Create chunk
                chunk_text = ' '.join(current_chunk)
                chunk = DocumentChunk(
                    document_id=document.id,
                    kb_id=document.kb_id,
                    chunk_index=len(chunks),
                    content=chunk_text,
                    char_start=char_start,
                    char_end=char_start + len(chunk_text),
                    metadata={"chunking_strategy": "sentence"}
                )
                chunks.append(chunk)
                
                # Reset
                char_start += len(chunk_text) + 1
                current_chunk = [sentence]
                current_size = sentence_size
            else:
                current_chunk.append(sentence)
                current_size += sentence_size + 1
        
        # Add final chunk
        if current_chunk:
            chunk_text = ' '.join(current_chunk)
            chunk = DocumentChunk(
                document_id=document.id,
                kb_id=document.kb_id,
                chunk_index=len(chunks),
                content=chunk_text,
                char_start=char_start,
                char_end=char_start + len(chunk_text),
                metadata={"chunking_strategy": "sentence"}
            )
            chunks.append(chunk)
        
        return chunks
    
    def _chunk_by_paragraph(self, document: Document, max_chunk_size: int) -> List[DocumentChunk]:
        """Chunk by paragraphs"""
        paragraphs = document.content.split('\n\n')
        
        chunks = []
        current_chunk = []
        current_size = 0
        char_start = 0
        
        for para in paragraphs:
            para = para.strip()
            if not para:
                continue
            
            para_size = len(para)
            
            if current_size + para_size > max_chunk_size and current_chunk:
                # Create chunk
                chunk_text = '\n\n'.join(current_chunk)
                chunk = DocumentChunk(
                    document_id=document.id,
                    kb_id=document.kb_id,
                    chunk_index=len(chunks),
                    content=chunk_text,
                    char_start=char_start,
                    char_end=char_start + len(chunk_text),
                    metadata={"chunking_strategy": "paragraph"}
                )
                chunks.append(chunk)
                
                # Reset
                char_start += len(chunk_text) + 2
                current_chunk = [para]
                current_size = para_size
            else:
                current_chunk.append(para)
                current_size += para_size + 2
        
        # Add final chunk
        if current_chunk:
            chunk_text = '\n\n'.join(current_chunk)
            chunk = DocumentChunk(
                document_id=document.id,
                kb_id=document.kb_id,
                chunk_index=len(chunks),
                content=chunk_text,
                char_start=char_start,
                char_end=char_start + len(chunk_text),
                metadata={"chunking_strategy": "paragraph"}
            )
            chunks.append(chunk)
        
        return chunks
    
    async def _read_file(self, file_path: str, doc_type: Optional[str] = None) -> str:
        """Read content from a file"""
        path = Path(file_path)
        
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")
        
        # Determine type from extension if not provided
        if not doc_type:
            doc_type = path.suffix.lower()[1:]  # Remove the dot
        
        if doc_type == "pdf":
            return self._read_pdf(path)
        elif doc_type == "docx":
            return self._read_docx(path)
        elif doc_type in ["txt", "md", "markdown"]:
            return path.read_text(encoding='utf-8')
        elif doc_type == "html":
            return self._extract_text_from_html(path.read_text(encoding='utf-8'))
        elif doc_type == "json":
            data = json.loads(path.read_text(encoding='utf-8'))
            return json.dumps(data, indent=2)
        elif doc_type == "csv":
            return self._read_csv(path)
        else:
            # Try to read as text
            return path.read_text(encoding='utf-8')
    
    def _read_pdf(self, path: Path) -> str:
        """Extract text from PDF"""
        reader = PdfReader(str(path))
        text_parts = []
        
        for page in reader.pages:
            text = page.extract_text()
            if text:
                text_parts.append(text)
        
        return '\n\n'.join(text_parts)
    
    def _read_docx(self, path: Path) -> str:
        """Extract text from DOCX"""
        doc = DocxDocument(str(path))
        paragraphs = []
        
        for para in doc.paragraphs:
            if para.text.strip():
                paragraphs.append(para.text)
        
        return '\n\n'.join(paragraphs)
    
    def _read_csv(self, path: Path) -> str:
        """Convert CSV to readable text"""
        with open(path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            rows = list(reader)
            
        if not rows:
            return ""
        
        # Format as structured text
        text_parts = []
        for i, row in enumerate(rows):
            text_parts.append(f"Row {i+1}:")
            for key, value in row.items():
                text_parts.append(f"  {key}: {value}")
            text_parts.append("")
        
        return '\n'.join(text_parts)
    
    def _extract_text_from_html(self, html: str) -> str:
        """Extract text from HTML"""
        soup = BeautifulSoup(html, 'html.parser')
        
        # Remove script and style elements
        for script in soup(["script", "style"]):
            script.decompose()
        
        # Get text
        text = soup.get_text()
        
        # Clean up whitespace
        lines = (line.strip() for line in text.splitlines())
        chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
        text = '\n'.join(chunk for chunk in chunks if chunk)
        
        return text
    
    async def _fetch_url(self, url: str) -> str:
        """Fetch content from URL"""
        try:
            response = await self.http_client.get(url)
            response.raise_for_status()
            
            content_type = response.headers.get('content-type', '').lower()
            
            if 'text/html' in content_type:
                return self._extract_text_from_html(response.text)
            elif 'application/pdf' in content_type:
                # Save to temp file and process
                # In production, handle this properly
                raise NotImplementedError("PDF URL processing not implemented")
            else:
                return response.text
                
        except Exception as e:
            logger.error("Failed to fetch URL", url=url, error=str(e))
            raise
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.http_client.aclose()