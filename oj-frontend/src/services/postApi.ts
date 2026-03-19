import axios from "axios";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

type PostId = string;

const unwrapResponse = <T>(res: BaseResponse<T>, defaultMessage: string): T => {
  if (res.code !== 0) {
    throw new Error(res.message || defaultMessage);
  }
  return res.data;
};

export const getPostDetail = async (id: PostId) => {
  const { data } = await axios.get<BaseResponse<any>>("/api/post/get/vo", {
    params: { id },
  });
  return unwrapResponse(data, "帖子加载失败");
};

export const deletePost = async (id: PostId) => {
  const { data } = await axios.post<BaseResponse<boolean>>("/api/post/delete", {
    id,
  });
  return unwrapResponse(data, "删除失败");
};

export const listPostComments = async (postId: PostId) => {
  const { data } = await axios.post<BaseResponse<any>>(
    "/api/post_comment/list/page/vo",
    {
      postId,
      current: 1,
      pageSize: 20,
    }
  );
  return unwrapResponse(data, "评论加载失败");
};

export const addPostComment = async (postId: PostId, content: string) => {
  const { data } = await axios.post<BaseResponse<number>>(
    "/api/post_comment/add",
    {
      postId,
      content,
    }
  );
  return unwrapResponse(data, "评论失败");
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
