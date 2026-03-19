import axios from "axios";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

export interface HomeEventItem {
  id: number;
  contestType: string;
  title: string;
  eventTime: string;
  location?: string;
  eventUrl?: string;
  description?: string;
  sortOrder?: number;
  userId?: number;
}

interface HomeEventPage {
  records: HomeEventItem[];
  total: number;
  current: number;
  size: number;
}

export interface HomeEventQueryRequest {
  title?: string;
  contestType?: string;
  current?: number;
  pageSize?: number;
  sortField?: string;
  sortOrder?: string;
}

export interface HomeEventAddPayload {
  contestType: string;
  title: string;
  eventTime: string;
  location?: string;
  eventUrl?: string;
  description?: string;
  sortOrder?: number;
}

export interface HomeEventUpdatePayload extends HomeEventAddPayload {
  id: number;
}

const unwrapResponse = <T>(res: BaseResponse<T>, defaultMessage: string): T => {
  if (res.code !== 0) {
    throw new Error(res.message || defaultMessage);
  }
  return res.data;
};

export const listUpcomingHomeEvents = async (limit = 20) => {
  const { data } = await axios.get<BaseResponse<HomeEventItem[]>>(
    "/api/homeEvent/list/upcoming",
    {
      params: { limit },
    }
  );
  return unwrapResponse(data, "加载主页比赛失败");
};

export const listHomeEventByPage = async (params: HomeEventQueryRequest) => {
  const { data } = await axios.post<BaseResponse<HomeEventPage>>(
    "/api/homeEvent/list/page",
    params
  );
  return unwrapResponse(data, "加载比赛配置失败");
};

export const addHomeEvent = async (payload: HomeEventAddPayload) => {
  const { data } = await axios.post<BaseResponse<number>>(
    "/api/homeEvent/add",
    payload
  );
  return unwrapResponse(data, "新增比赛失败");
};

export const updateHomeEvent = async (payload: HomeEventUpdatePayload) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/homeEvent/update",
    payload
  );
  return unwrapResponse(data, "更新比赛失败");
};

export const deleteHomeEvent = async (id: number) => {
  const { data } = await axios.post<BaseResponse<boolean>>(
    "/api/homeEvent/delete",
    {
      id,
    }
  );
  return unwrapResponse(data, "删除比赛失败");
};

interface ErrorMessageCarrier {
  message?: string;
  response?: {
    data?: {
      message?: string;
    };
  };
  body?: {
    message?: string;
  };
}

export const getErrorMessage = (
  error: unknown,
  fallback = "操作失败，请稍后重试"
) => {
  const normalizedError = (error || {}) as ErrorMessageCarrier;
  return (
    normalizedError.response?.data?.message ||
    normalizedError.body?.message ||
    normalizedError.message ||
    fallback
  );
};
