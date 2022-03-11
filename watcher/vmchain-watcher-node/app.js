'use strict';

const { env } = require('process');
const useMysql = app => {
  const mysqlConfig = {
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USERNAME,
    password: env.DB_PASSWORD,
    database: env.DB_NAME,
    app: true,
    agent: false,
  };
  app.mysql = app.mysql.createInstance(mysqlConfig);
};


module.exports = app => {
  app.beforeStart(async () => {
    useMysql(app);
  });
};
