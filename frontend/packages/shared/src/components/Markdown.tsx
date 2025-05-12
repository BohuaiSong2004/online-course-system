import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

/** Renders chapter Markdown on the server. Raw HTML in the source is not rendered (XSS-safe default). */
export function Markdown({ source }: { source: string }) {
  return (
    <div className="prose prose-slate max-w-none prose-headings:font-semibold prose-pre:bg-slate-900 prose-a:text-brand-700">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{source}</ReactMarkdown>
    </div>
  );
}
