import axios from "axios";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

export interface QuestionDiscussionVO {
  id: string;
  questionId: string;
  content: string;
  thumbNum?: number;
  userId?: string;
  createTime?: string;
  updateTime?: string;
  hasThumb?: boolean;
  user?: {
    id?: string;
    userName?: string;
    userAvatar?: string;
    userRole?: string;
  };
}

interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

const unwrapResponse = <T>(res: BaseResponse<T>, defaultMessage: string): T => {
  if (res.code !== 0) {
    throw new Error(res.message || defaultMessage);
  }
  return res.data;
};

export const listQuestionDiscussions = async (
  questionId: string,
  current = 1,
  pageSize = 20
) => {
  const { data } = await axios.post<
    BaseResponse<PageResult<QuestionDiscussionVO>>
  >("/api/question_discussion/list/page/vo", {
    questionId,
    current,
    pageSize,
  });
  return unwrapResponse(data, "加载讨论失败");
};

export const addQuestionDiscussion = async (
  questionId: string,
  content: string
) => {
  const { data } = await axios.post<BaseResponse<string>>(
    "/api/question_discussion/add",
    {
      questionId,
      content,
    }
  );
  return unwrapResponse(data, "发布讨论失败");
};

export const deleteQuestionDiscussion = async (id: string) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/question_discussion/delete",
    {
      id,
    }
  );
  return unwrapResponse(data, "删除讨论失败");
};

export const doQuestionDiscussionThumb = async (discussionId: string) => {
  const { data } = await axios.post<BaseResponse<number>>(
    "/api/question_discussion_thumb/",
    {
      discussionId,
    }
  );
  return unwrapResponse(data, "点赞失败");
};

export const getQuestionDiscussionError = (
  error: any,
  fallback = "操作失败，请稍后重试"
) => {
  return (
    error?.response?.data?.message ||
    error?.body?.message ||
    error?.message ||
    fallback
  );
};
