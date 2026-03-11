package com.sjj.oj_backend.model.dto.post;

import com.sjj.oj_backend.model.entity.Post;
import com.sjj.oj_backend.utils.PostContentCodec;
import lombok.Data;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 帖子 ES 包装类
 *
 * 
 * 
 **/
// todo 取消注释开启 ES（须先配置 ES）
//@Document(indexName = "post")
@Data
public class PostEsDTO implements Serializable {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 图片列表
     */
    private List<String> images;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     *
     * @param post
     * @return
     */
    public static PostEsDTO objToDto(Post post) {
        if (post == null) {
            return null;
        }
        PostEsDTO postEsDTO = new PostEsDTO();
        BeanUtils.copyProperties(post, postEsDTO);
        postEsDTO.setTags(PostContentCodec.decodeTags(post.getTags()));
        postEsDTO.setImages(PostContentCodec.decodeImages(post.getImages()));
        return postEsDTO;
    }

    /**
     * 包装类转对象
     *
     * @param postEsDTO
     * @return
     */
    public static Post dtoToObj(PostEsDTO postEsDTO) {
        if (postEsDTO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postEsDTO, post);
        post.setTags(PostContentCodec.encodeTags(postEsDTO.getTags()));
        post.setImages(PostContentCodec.encodeImages(postEsDTO.getImages()));
        return post;
    }
}
