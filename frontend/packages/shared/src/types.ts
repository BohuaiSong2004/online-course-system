// Mirrors the Spring Boot DTOs (backend/src/main/java/com/bohuai/ocs/dto, model).

export type Role = "LEARNER" | "MANAGER" | "INSTRUCTOR" | "ADMIN";
export const ROLES: Role[] = ["LEARNER", "MANAGER", "INSTRUCTOR", "ADMIN"];
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
  videoUrl?: string | null;
  position: number;
  durationMin: number;
  preview: boolean;
  quizPassPercent?: number;
  quizQuestionCount?: number;
}

export interface QuizOption {
  id: number;
  position: number;
  text: string;
  correct?: boolean; // only present in studio responses
}

export interface QuizQuestion {
  id: number;
  position: number;
  prompt: string;
  explanation?: string | null;
  options: QuizOption[];
}

export interface QuizAttempt {
  id: number;
  score: number;
  total: number;
  passed: boolean;
  createdAt: string;
}

export interface QuizResult {
  score: number;
  total: number;
  passed: boolean;
  passPercent: number;
  results: { questionId: number; correct: boolean; correctOptionId: number | null; explanation: string | null }[];
  progress: ProgressResponse | null;
}

export interface Certificate {
  id: number;
  code: string;
  learnerName: string;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  instructorName: string;
  totalMinutes: number;
  issuedAt: string;
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
  certificateCode: string | null;
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
  quiz: QuizQuestion[];
  bestAttempt: QuizAttempt | null;
}

export interface ProgressResponse {
  completedChapters: number;
  totalChapters: number;
  courseCompleted: boolean;
  nextChapterId: number | null;
  certificateCode: string | null;
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
  dueDate: string | null;
  assignedByName: string | null;
}

export interface Activity {
  id: number;
  courseId: number | null;
  chapterId: number | null;
  action:
    | "ENROLL"
    | "UNENROLL"
    | "VIEW_CHAPTER"
    | "COMPLETE_CHAPTER"
    | "COMPLETE_COURSE"
    | "ASSIGNED"
    | "QUIZ_PASSED"
    | "QUIZ_FAILED";
  createdAt: string;
  courseTitle: string | null;
  chapterTitle: string | null;
}

export interface ApiErrorBody {
  error: string;
  message: string;
  fields?: Record<string, string>;
}

// ---------- admin ----------

export interface AdminUser {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  teamId: number | null;
  teamName: string | null;
  active: boolean;
  createdAt: string;
}

export interface Team {
  id: number;
  name: string;
  managerId: number | null;
  managerName: string | null;
  memberCount: number;
}

export type AssignmentStatus = "COMPLETED" | "OVERDUE" | "IN_PROGRESS" | "NOT_STARTED";

export interface AssignmentRow {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  teamName: string | null;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  dueDate: string | null;
  assignedAt: string;
  assignedByName: string | null;
  completedAt: string | null;
  totalChapters: number;
  completedChapters: number;
  status: AssignmentStatus;
}

export interface CourseStat {
  courseId: number;
  title: string;
  slug: string;
  assigned: number;
  enrolled: number;
  completed: number;
  overdue: number;
  completionRate: number;
}

export interface TeamStat {
  teamId: number;
  name: string;
  managerName: string | null;
  members: number;
  assigned: number;
  completed: number;
  overdue: number;
  completionRate: number;
}

export interface ReportOverview {
  learners: number;
  assignments: number;
  completed: number;
  overdue: number;
  completionRate: number;
  courses: CourseStat[];
  teams: TeamStat[];
}

export interface AssignResult {
  created: number;
  updated: number;
  unchanged: number;
  skipped: number;
}
