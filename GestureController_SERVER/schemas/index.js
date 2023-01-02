const mysql = require('mysql');

const connection = mysql.createConnection({
    host: '127.0.0.1',
    user: 'root',
    password: 'PW',
    database:'ipl_gesture'
});

connection.on('error', (error) => {
    console.log('데이터베이스 연결 에러\n', error);
});

module.exports = connection;