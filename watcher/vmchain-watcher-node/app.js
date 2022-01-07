'use strict';

const { env } = require('process');
const useMysql = app => {
  const mysqlConfig = {
    // host: env.DB_HOST,
    // port: env.DB_PORT,
    // user: env.DB_USERNAME,
    // password: env.DB_PASSWORD,
    // database: env.DB_NAME,
    host: 'exchange-mysql5-7.database.svc.cluster.local',
    port: 3306,
    user: 'root',
    password: 'ZxcvAsdf',
    database: 'chainscan',
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
