FROM node:16.3.0
WORKDIR /app
COPY . .
RUN pwd
RUN ls
# RUN npm cache clean --force
RUN npm config set registry https://registry.npm.taobao.org
RUN npm install cnpm -g
RUN cnpm install tyarn -g
# RUN yarn add @polkadot/api@5.8.3
# RUN yarn add ethers@5.5.3
# RUN yarn add zksync-crypto@0.6.0
# RUN yarn add ethereumjs-tx@2.1.2
# RUN yarn add web3
RUN tyarn

# 启动服务，默认挂载到7011端口
EXPOSE 7011
CMD ["npm run start"]
