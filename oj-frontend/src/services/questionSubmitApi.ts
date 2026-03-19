import axios from "axios";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface AcceptedQuestionSubmitItem {
  submitId: number;
  questionId: number;
  questionNo?: number;
  questionTitle?: string;
  language?: string;
  code?: string;
  submitTime?: string;
}

export interface AcceptedQuestionSubmitQuery {
  userId: string | number;
  current?: number;
  pageSize?: number;
}

export const listAcceptedQuestionSubmitByPage = async (
  params: AcceptedQuestionSubmitQuery
) => {
  const { data } = await axios.post<
    BaseResponse<PageResult<AcceptedQuestionSubmitItem>>
  >("/api/question_submit/accepted/list/page", params);

  if (data.code !== 0) {
    throw new Error(data.message || "Load accepted submissions failed");
  }

  return (
    data.data || {
      records: [],
      total: 0,
      current: Number(params.current || 1),
      size: Number(params.pageSize || 5),
    }
  );
};
