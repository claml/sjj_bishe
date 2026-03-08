export const PASSWORD_MIN_LENGTH = 8;

export const PASSWORD_RULE_HINT = `密码不少于 ${PASSWORD_MIN_LENGTH} 位`;

/**
 * 与注册页保持一致的密码规则校验
 */
export const validatePasswordByRegisterRule = (
  password: string
): string | null => {
  if (password.length < PASSWORD_MIN_LENGTH) {
    return PASSWORD_RULE_HINT;
  }
  return null;
};
