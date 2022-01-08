const useSuccess = (ctx, params) => {
  const res = { code: 200, data: [], message: 'success' };
  ctx.body = Object.assign(res, { ...params });
  ctx.status = 200;
};

module.exports = {
  useSuccess,
};
