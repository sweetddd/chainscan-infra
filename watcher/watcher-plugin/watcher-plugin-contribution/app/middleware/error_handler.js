'use strict';

module.exports = (option, app) => {
  const validateToken = async (ctx, token) => {
    if (!token) {
      return false;
    }
    const url = 'http://xadmin.powx.io/api/system/auth/token';
    const payload = {
      method: 'post',
      timeout: [ '30s', '30s' ],
      dataType: 'json',
      token,
      data: {
        token,
        permissionNameList: [ 'explorer:chain' ],
        type: 'ALL',
      },
      contentType: 'json',
    };
    return ctx.curl(url, payload);
  };

  return async function(ctx, next) {
    try {
      const token = ctx.request.header['x-auth-token'];
      const result = await validateToken(ctx, token);
      if (!result.data.code) {
        await next();
      } else {
        ctx.body = { error: 'permission denied' };
        ctx.status = 200;
      }
    } catch (err) {
      // 所有的异常都在 app 上触发一个 error 事件，框架会记录一条错误日志
      app.emit('error', err, this);
      const status = err.status || 500;
      // 生产环境时 500 错误的详细错误内容不返回给客户端，因为可能包含敏感信息
      const error = status === 500 && app.config.env === 'prod'
        ? 'Internal Server Error'
        : err.message;
      // 从 error 对象上读出各个属性，设置到响应中
      ctx.body = { error };
      if (status === 422) {
        ctx.body.detail = err.errors;
      }
      ctx.status = status;
    }
  };
};
