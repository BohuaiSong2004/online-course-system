/**
 * Embeds YouTube / Vimeo links as a privacy-enhanced iframe; any other https link is shown as a link
 * (arbitrary sites are never framed).
 */
export function toEmbedUrl(url: string): string | null {
  try {
    const u = new URL(url);
    if (u.protocol !== "https:") return null;
    const host = u.hostname.replace(/^www\./, "");
    if (host === "youtube.com" || host === "m.youtube.com") {
      const id = u.searchParams.get("v");
      return id && /^[\w-]{6,20}$/.test(id) ? `https://www.youtube-nocookie.com/embed/${id}` : null;
    }
    if (host === "youtu.be") {
      const id = u.pathname.slice(1);
      return /^[\w-]{6,20}$/.test(id) ? `https://www.youtube-nocookie.com/embed/${id}` : null;
    }
    if (host === "vimeo.com") {
      const id = u.pathname.split("/").filter(Boolean)[0];
      return id && /^\d+$/.test(id) ? `https://player.vimeo.com/video/${id}` : null;
    }
    return null;
  } catch {
    return null;
  }
}

export function Video({ url, title }: { url: string; title: string }) {
  const embed = toEmbedUrl(url);
  if (!embed) {
    return (
      <a href={url} target="_blank" rel="noopener noreferrer" className="btn-secondary">
        ▶ Watch the video for this chapter
      </a>
    );
  }
  return (
    <div className="aspect-video w-full overflow-hidden rounded-xl bg-slate-900">
      <iframe
        src={embed}
        title={`Video: ${title}`}
        className="h-full w-full"
        loading="lazy"
        allow="accelerometer; encrypted-media; gyroscope; picture-in-picture; fullscreen"
        referrerPolicy="strict-origin-when-cross-origin"
        allowFullScreen
      />
    </div>
  );
}
