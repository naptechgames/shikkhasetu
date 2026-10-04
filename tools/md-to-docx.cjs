// Converts a simple Markdown file (headings, paragraphs, lists, tables, code blocks) to .docx.
// Usage:  node tools/md-to-docx.cjs docs/PROJECT_REPORT.md ShikkhaSetu_Project_Report.docx
const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, WidthType, HeadingLevel, ShadingType } =
  require('C:/Users/musab/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/docx');

const [input, output] = process.argv.slice(2);
const lines = fs.readFileSync(input, 'utf8').replace(/\r/g, '').split('\n');
const blue = '173B59';

// **bold** and `code` inside a line become formatted runs.
function runs(text, base = {}) {
  return text.split(/(\*\*[^*]+\*\*|`[^`]+`)/).filter(Boolean).map((part) => {
    if (part.startsWith('**')) return new TextRun({ ...base, text: part.slice(2, -2), bold: true });
    if (part.startsWith('`')) return new TextRun({ ...base, text: part.slice(1, -1), font: 'Consolas' });
    return new TextRun({ ...base, text: part });
  });
}
const cells = (line) => line.trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());

const children = [];
for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  if (line.startsWith('```')) {
    for (i++; i < lines.length && !lines[i].startsWith('```'); i++) {
      children.push(new Paragraph({ spacing: { after: 0 }, shading: { type: ShadingType.CLEAR, fill: 'F2F4F6' },
        children: [new TextRun({ text: lines[i] || ' ', font: 'Consolas', size: 18 })] }));
    }
    children.push(new Paragraph({ children: [] }));
  } else if (line.startsWith('|')) {
    const rows = [];
    for (; i < lines.length && lines[i].startsWith('|'); i++) {
      if (!/^\|[\s:|-]+\|$/.test(lines[i].trim())) rows.push(cells(lines[i]));
    }
    i--;
    const columns = Math.max(...rows.map((r) => r.length));
    const width = Math.floor(9360 / columns);
    const header = rows[0].some((c) => c !== '');
    children.push(new Table({
      width: { size: 9360, type: WidthType.DXA },
      columnWidths: Array(columns).fill(width),
      rows: rows.filter((r, n) => n > 0 || header).map((r, n) => new TableRow({
        cantSplit: true,
        children: Array.from({ length: columns }, (_, c) => new TableCell({
          width: { size: width, type: WidthType.DXA },
          margins: { top: 70, bottom: 70, left: 100, right: 100 },
          shading: header && n === 0 ? { fill: blue, type: ShadingType.CLEAR } : undefined,
          children: [new Paragraph({ spacing: { after: 0 },
            children: runs(r[c] || '', header && n === 0 ? { size: 19, bold: true, color: 'FFFFFF' } : { size: 19 }) })],
        })),
      })),
    }));
    children.push(new Paragraph({ children: [] }));
  } else if (line.startsWith('# ')) {
    children.push(new Paragraph({ heading: HeadingLevel.TITLE, children: runs(line.slice(2)) }));
  } else if (line.startsWith('## ')) {
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, keepNext: true, spacing: { before: 240, after: 100 }, children: runs(line.slice(3)) }));
  } else if (/^\* /.test(line)) {
    let text = line.slice(2);
    while (i + 1 < lines.length && /^  \S/.test(lines[i + 1])) text += ' ' + lines[++i].trim();
    children.push(new Paragraph({ bullet: { level: 0 }, spacing: { after: 60 }, children: runs(text) }));
  } else if (/^\d+\. /.test(line)) {
    let text = line;
    while (i + 1 < lines.length && /^   \S/.test(lines[i + 1])) text += ' ' + lines[++i].trim();
    children.push(new Paragraph({ indent: { left: 360, hanging: 360 }, spacing: { after: 60 }, children: runs(text) }));
  } else if (line.trim() !== '') {
    let text = line;
    while (i + 1 < lines.length && lines[i + 1].trim() !== '' && !/^(#|\||\*|\d+\.|```)/.test(lines[i + 1])) text += ' ' + lines[++i];
    children.push(new Paragraph({ spacing: { after: 120 }, children: runs(text) }));
  }
}

const doc = new Document({
  styles: {
    default: { document: { run: { font: 'Calibri', size: 22, color: '24313C' } } },
    paragraphStyles: [
      { id: 'Title', name: 'Title', basedOn: 'Normal', next: 'Normal', run: { size: 40, bold: true, color: blue }, paragraph: { spacing: { after: 200 } } },
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 27, bold: true, color: blue } },
    ],
  },
  sections: [{ properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1050, bottom: 1000, left: 1273, right: 1273 } } }, children }],
});
Packer.toBuffer(doc).then((buffer) => { fs.writeFileSync(output, buffer); console.log(output + ' generated'); });
