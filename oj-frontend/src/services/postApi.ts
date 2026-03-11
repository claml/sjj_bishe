import axios from "axios";
import { PostControllerService, type DeleteRequest } from "../../generated";

interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

const unwrapResponse = <T>(res: BaseResponse<T>, defaultMessage: string): T => {
  if (res.code !== 0) {
    throw new Error(res.message || defaultMessage);
  }
  return res.data;
};

export const getPostDetail = async (id: number) => {
  const res = await PostControllerService.getPostVoByIdUsingGet(id);
  return unwrapResponse(res as BaseResponse<any>, "帖子加载失败");
};

export const deletePost = async (id: number) => {
  const req: DeleteRequest = { id };
  const res = await PostControllerService.deletePostUsingPost(req);
  return unwrapResponse(res as BaseResponse<boolean>, "删除失败");
};

export const listPostComments = async (postId: number) => {
  const { data } = await axios.post<BaseResponse<any>>(
    "/api/post_comment/list/page/vo",
    {
      postId,
      current: 1,
      pageSize: 100,
    }
  );
  return unwrapResponse(data, "评论加载失败");
};

export const addPostComment = async (postId: number, content: string) => {
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
