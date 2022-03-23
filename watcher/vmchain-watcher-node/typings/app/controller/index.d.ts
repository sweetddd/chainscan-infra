// This file is created by egg-ts-helper@1.30.2
// Do not modify this file!!!!!!!!!

import 'egg';
import ExportV1SubscanJob = require('../../../app/controller/v1/subscanJob');

declare module 'egg' {
  interface IController {
    v1: {
      subscanJob: ExportV1SubscanJob;
    }
  }
}
