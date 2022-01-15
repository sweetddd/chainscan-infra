'use strict';

/**
 * @param {Egg.Application} app - egg application
 */
module.exports = app => {
    const {router, controller} = app;
    router.resources('subscanJob', '/api/v1/subscanJob', controller.v1.subscanJob);
};
