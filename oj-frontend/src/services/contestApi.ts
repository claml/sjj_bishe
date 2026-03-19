import axios from "axios";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

export interface ContestRankVO {
  rank: number;
  userId: number;
  userName?: string;
  userAvatar?: string;
  solvedCount: number;
  lastAcceptedTime?: string;
}

export interface ContestQuestionVO {
  id: number;
  title: string;
  tags?: string[];
  submitNum?: number;
  acceptedNum?: number;
}

export interface ContestVO {
  id: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  status: "not_started" | "running" | "ended";
  joined?: boolean;
  questionList?: ContestQuestionVO[];
  solvedQuestionIdList?: number[];
}

export interface ContestPage {
  records: ContestVO[];
  total: number;
  current: number;
  size: number;
}

export interface QuestionBankItem {
  id: number;
  title: string;
}

interface QuestionBankPage {
  records: QuestionBankItem[];
  total: number;
  current: number;
  size: number;
}

export interface ContestQueryRequest {
  title?: string;
  status?: "not_started" | "running" | "ended";
  current?: number;
  pageSize?: number;
}

interface JudgeConfigPayload {
  memoryLimit: number;
  stackLimit: number;
  timeLimit: number;
}

interface JudgeCasePayload {
  input: string;
  output: string;
}

export interface NewContestQuestionPayload {
  title: string;
  content: string;
  answer: string;
  tags: string[];
  judgeConfig: JudgeConfigPayload;
  judgeCase: JudgeCasePayload[];
}

export interface ContestAddPayload {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  inviteCode?: string;
  invitedUserIdList?: number[];
  questionIdList: number[];
  newQuestionList: NewContestQuestionPayload[];
}

export interface ContestUpdatePayload {
  id: number;
  title?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
}

const unwrapResponse = <T>(res: BaseResponse<T>, defaultMessage: string): T => {
  if (res.code !== 0) {
    throw new Error(res.message || defaultMessage);
  }
  return res.data;
};

export const listContestVOByPage = async (params: ContestQueryRequest) => {
  const { data } = await axios.post<BaseResponse<ContestPage>>(
    "/api/contest/list/page/vo",
    params
  );
  return unwrapResponse(data, "加载竞赛列表失败");
};

export const getContestDetailWithInviteCode = async (
  id: string | number,
  inviteCode?: string
) => {
  const { data } = await axios.get<BaseResponse<ContestVO>>(
    "/api/contest/get/vo",
    {
      params: { id, inviteCode },
    }
  );
  return unwrapResponse(data, "加载竞赛详情失败");
};

export const getContestDetail = async (id: string | number) => {
  return getContestDetailWithInviteCode(id);
};

export const joinContest = async (contestId: number, inviteCode?: string) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/contest/join",
    {
      contestId,
      inviteCode,
    }
  );
  return unwrapResponse(data, "参加竞赛失败");
};

export const getContestRank = async (contestId: string | number) => {
  const { data } = await axios.get<BaseResponse<ContestRankVO[]>>(
    "/api/contest/rank",
    {
      params: { contestId },
    }
  );
  return unwrapResponse(data, "加载排行榜失败");
};

export const addContest = async (payload: ContestAddPayload) => {
  const { data } = await axios.post<BaseResponse<number>>(
    "/api/contest/add",
    payload
  );
  return unwrapResponse(data, "创建竞赛失败");
};

export const updateContest = async (payload: ContestUpdatePayload) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/contest/update",
    payload
  );
  return unwrapResponse(data, "更新竞赛失败");
};

export const deleteContest = async (id: number) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/contest/delete",
    {
      id,
    }
  );
  return unwrapResponse(data, "删除竞赛失败");
};

export const listQuestionBank = async (title = "") => {
  const mergedRecords: QuestionBankItem[] = [];
  let current = 1;
  const pageSize = 20;
  let total = 0;
  let hasMore = true;

  while (hasMore) {
    const { data } = await axios.post<BaseResponse<QuestionBankPage>>(
      "/api/question/list/page/vo",
      {
        title,
        current,
        pageSize,
      }
    );
    const pageData = unwrapResponse(data, "加载题库失败");
    const pageRecords = pageData.records ?? [];
    total = Number(pageData.total ?? 0);
    mergedRecords.push(...pageRecords);

    hasMore = mergedRecords.length < total && pageRecords.length > 0;
    if (hasMore) {
      current += 1;
    }
  }

  return {
    records: mergedRecords,
    total,
    current: 1,
    size: mergedRecords.length,
  } as QuestionBankPage;
};

interface InviteUserItem {
  id: string;
  userName?: string;
}

interface InviteUserPage {
  records: InviteUserItem[];
  total: number;
  current: number;
  size: number;
}

export interface InviteUserOption {
  id: number;
  userName: string;
}

export const listInviteUsers = async (userName = "") => {
  const mergedRecords: InviteUserOption[] = [];
  let current = 1;
  const pageSize = 20;
  let total = 0;
  let hasMore = true;

  while (hasMore) {
    const { data } = await axios.post<BaseResponse<InviteUserPage>>(
      "/api/user/list/page/vo",
      {
        userName,
        current,
        pageSize,
      }
    );
    const pageData = unwrapResponse(data, "加载用户列表失败");
    const pageRecords = pageData.records ?? [];
    total = Number(pageData.total ?? 0);
    mergedRecords.push(
      ...pageRecords
        .map((item) => ({
          id: Number(item.id),
          userName: item.userName || `用户${item.id}`,
        }))
        .filter((item) => Number.isFinite(item.id) && item.id > 0)
    );

    hasMore = mergedRecords.length < total && pageRecords.length > 0;
    if (hasMore) {
      current += 1;
    }
  }

  return mergedRecords;
};

export const getErrorMessage = (
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
