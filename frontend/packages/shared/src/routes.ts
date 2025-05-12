// Cross-zone URLs. Each zone is a separate Next.js app, so navigation between zones must be a
// full page load (<a href>), while navigation inside a zone can use next/link.
export const routes = {
  home: "/",
  login: (next?: string) => (next ? `/login?next=${encodeURIComponent(next)}` : "/login"),
  register: (next?: string) => (next ? `/register?next=${encodeURIComponent(next)}` : "/register"),
  catalog: "/courses",
  course: (slug: string) => `/courses/${slug}`,
  dashboard: "/learn",
  chapter: (slug: string, chapterId: number) => `/learn/${slug}/${chapterId}`,
  studio: "/studio",
  studioCourse: (id: number) => `/studio/courses/${id}`,
  studioChapter: (courseId: number, chapterId: number) => `/studio/courses/${courseId}/chapters/${chapterId}`,
};
