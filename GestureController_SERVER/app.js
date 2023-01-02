const express = require('express');
const runSocket = require('./socket.js');  // 소켓 모듈 가져오기
const connection = require('./schemas/index');

const app = express();

// 서버 포트 설정
app.set('port', 8080);
app.set('connection', connection);

//mysql 연결
connection.connect();

// 서버 실행
const server = app.listen(app.get('port'), () => {
    console.log(app.get('port'), '번 포트에서 서버 대기중');
});

// 소켓 모듈에 서버와
runSocket(server, app);

