import { RouteRecordRaw } from "vue-router";
import UserLayout from "@/layouts/UserLayout.vue";
import UserLoginView from "@/views/user/UserLoginView.vue";
import UserRegisterView from "@/views/user/UserRegisterView.vue";
import NoAuthView from "@/views/NoAuthView.vue";
import ACCESS_ENUM from "@/access/accessEnum";
import AddQuestionView from "@/views/question/AddQuestionView.vue";
import ManageQuestionView from "@/views/question/ManageQuestionView.vue";
import QuestionsView from "@/views/question/QuestionsView.vue";
import ViewQuestionView from "@/views/question/ViewQuestionView.vue";
import UpdateQuestionView from "@/views/question/UpdateQuestionView.vue";
import UserAccountView from "@/views/user/UserAccountView.vue";
import DiscussionView from "@/views/post/DiscussionView.vue";
import PostDetailView from "@/views/post/PostDetailView.vue";
import UserProfileView from "@/views/user/UserProfileView.vue";
import ContestListView from "@/views/contest/ContestListView.vue";
import ContestDetailView from "@/views/contest/ContestDetailView.vue";
import ManageContestView from "@/views/contest/ManageContestView.vue";
import HomeView from "@/views/HomeView.vue";
import ManageHomeEventView from "@/views/ManageHomeEventView.vue";

export const routes: Array<RouteRecordRaw> = [
  {
    path: "/user",
    name: "用户",
    component: UserLayout,
    children: [
      {
        path: "/user/login",
        name: "用户登录",
        component: UserLoginView,
      },
      {
        path: "/user/register",
        name: "用户注册",
        component: UserRegisterView,
      },
    ],
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/",
    name: "首页",
    component: HomeView,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/questions",
    name: "题库",
    component: QuestionsView,
  },
  {
    path: "/contests",
    name: "竞赛",
    component: ContestListView,
    meta: {
      access: ACCESS_ENUM.USER,
    },
  },
  {
    path: "/contest/:id",
    name: "竞赛详情",
    component: ContestDetailView,
    props: true,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/view/question/:id",
    name: "在线做题",
    component: ViewQuestionView,
    props: true,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/add/question",
    name: "创建题目",
    component: AddQuestionView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
      hideInMenu: true,
    },
  },
  {
    path: "/update/question",
    name: "更新题目",
    component: UpdateQuestionView,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/user/account",
    name: "账号信息",
    component: UserAccountView,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/user/:id",
    name: "个人主页",
    component: UserProfileView,
    props: true,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/discussion",
    name: "帖子广场",
    component: DiscussionView,
  },
  {
    path: "/post/:id",
    name: "帖子详情",
    component: PostDetailView,
    props: true,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/manage/question/",
    name: "管理题库",
    component: ManageQuestionView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/manage/contest",
    name: "管理竞赛",
    component: ManageContestView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/manage/home-event",
    name: "管理首页活动",
    component: ManageHomeEventView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/noAuth",
    name: "无权限",
    component: NoAuthView,
    meta: {
      hideInMenu: true,
    },
  },
];
