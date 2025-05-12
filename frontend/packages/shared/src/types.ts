// Mirrors the Spring Boot DTOs (backend/src/main/java/com/bohuai/ocs/dto, model).

export type Role = "LEARNER" | "INSTRUCTOR" | "ADMIN";
export type Level = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export const LEVELS: Level[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

export interface UserView {
  id: number;
  email: string;
  displayName: string;
  role: Role;
}

export interface Course {
  id: number;
  slug: string;
  title: string;
  summary: string;
  description?: string | null;
  category: string;
  level: Level;
  instructorId: number;
  instructorName: string;
  status: "DRAFT" | "PUBLISHED";
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  chapterCount: number;
  totalMinutes: number;
}

export interface Chapter {
  id: number;
  courseId: number;
  title: string;
  content?: string | null;
  position: number;
  durationMin: number;
  preview: boolean;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ViewerState {
  signedIn: boolean;
  enrolled: boolean;
  enrollmentStatus: "ACTIVE" | "COMPLETED" | null;
  canEdit: boolean;
  completedChapterIds: number[];
}

export interface CourseDetail {
  course: Course;
  chapters: Chapter[];
  enrollmentCount: number;
  viewer: ViewerState;
}

export interface ChapterView {
  chapter: Chapter;
  courseSlug: string;
  courseTitle: string;
  prevChapterId: number | null;
  nextChapterId: number | null;
  completed: boolean;
  enrolled: boolean;
}

export interface ProgressResponse {
  completedChapters: number;
  totalChapters: number;
  courseCompleted: boolean;
  nextChapterId: number | null;
}

export interface EnrollmentView {
  courseId: number;
  slug: string;
  title: string;
  summary: string;
  category: string;
  level: Level;
  instructorName: string;
  status: "ACTIVE" | "COMPLETED";
  enrolledAt: string;
  completedAt: string | null;
  totalChapters: number;
  completedChapters: number;
  nextChapterId: number | null;
}

export interface Activity {
  id: number;
  courseId: number | null;
  chapterId: number | null;
  action: "ENROLL" | "UNENROLL" | "VIEW_CHAPTER" | "COMPLETE_CHAPTER" | "COMPLETE_COURSE";
  createdAt: string;
  courseTitle: string | null;
  chapterTitle: string | null;
}

export interface ApiErrorBody {
  error: string;
  message: string;
  fields?: Record<string, string>;
}
