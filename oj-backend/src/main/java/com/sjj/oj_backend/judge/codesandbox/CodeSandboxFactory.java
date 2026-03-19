package com.sjj.oj_backend.judge.codesandbox;

import com.sjj.oj_backend.judge.codesandbox.impl.ExampleCodeSandbox;
import com.sjj.oj_backend.judge.codesandbox.impl.RemoteCodeSandbox;
import com.sjj.oj_backend.judge.codesandbox.impl.ThirdPartyCodeSandbox;

/**
 * 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 */
public class CodeSandboxFactory {

    /**
     * 创建代码沙箱示例
     *
     * @param type 沙箱类型
     * @return
     */
    public static CodeSandbox newInstance(String type) {
        return newInstance(type, null, null, null);
    }

    public static CodeSandbox newInstance(String type, String remoteUrl, String authHeaderName, String authSecret) {
        switch (type) {
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox(remoteUrl, authHeaderName, authSecret);
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            default:
                return new ExampleCodeSandbox();
        }
    }
}
