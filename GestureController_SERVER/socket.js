const socketIO = require('socket.io');
const fs = require('fs');


module.exports = (server, app) => {
    // 익스프레스 서버와 소켓 연결
    const io = socketIO(server, {path:'/socket.io'}, {maxHttpBufferSize:1e8});
    // io.set('transports', ['websocket']);
    
    
    // 클라이언트 저장소 -> ir: 아두이노, phone: 앱, ml: ml
    const clients = {};
    const connection = app.get('connection');
    
    const getTime = () => {
        date = new Date();
        return date.toLocaleString();
    };
    
    const getSocketId = (client_name) => {
        return Object.keys(clients).find(key => clients[key] === client_name);
    };

    //클라이언트 있는지 체크 하고 메시지 전송
    const emit2one = (client_name, event_name, data) => {
        (getSocketId(client_name) == undefined)
            ? console.log(`[${getTime()}]: cant send data: ${client_name} is missing now \ndata: ${data}`)
            : io.to(getSocketId(client_name)).emit(event_name, data);
    };

    //클라이언트 접속
    io.on('connection', (socket) => {

        io.to(socket.id).emit('request name', {'msg':'send your name'});

        // 클라이언트 종료 이벤트
        socket.on('disconnect', () => {
            console.log(`[${getTime()}]: ${clients[socket.id]} 접속 종료`);

            if(clients[socket.id] == 'phone'){
                connection.query('DELETE FROM ipl_gesture.signal where deft is false', (error, result, fields) => {
                    if(error){
                        console.log(`[${getTime()}]: delete signal fail`);
                    } else {
                        console.log(`[${getTime()}]: mapping data deleted`);
                    }
                });
            }
            
            // 클라이언트 목록에서 해당 소켓 삭제
            delete clients[socket.id];       
            console.log(`[${getTime()}]: ${JSON.stringify(clients)}`);            
        });

        /* 클라이언트 등록 */
        socket.on('client registration', (data) => {
            clients[socket.id] = data['device']
            console.log(`[${getTime()}]: socket id: ${getSocketId(data['device'])} : ${clients[socket.id]} 등록 완료`);
            console.log(`[${getTime()}]: ${JSON.stringify(clients)}`);
        });


        /* 
            0. 서버 - 아두이노 이벤트 
        */
        // ir rawdata 받아서 디비 저장
        socket.on('get rawData', (data) => {
            const did = data['did'];
            const gid = data['gid'];
            const length = data['length'];
            const rawData = data['rawData'];            
            
            //디비 저장
            connection.query(`INSERT INTO ipl_gesture.signal(did, gid,length, rawdata, deft) VALUES(${did}, ${gid}, ${length}, "${rawData}", ${false})`, (error, results, fields) => {
                if (error) {                    
                    (error.errno == 1062) 
                        ? emit2one('phone', 'mapping result', {'did':did, 'gid':gid, 'result':2, 'msg':'Duplicate'})    // 중복된 제스처
                        : emit2one('phone', 'mapping result', {'did':did, 'gid':gid, 'result':0});    // 제스처 저장 실패
                    console.log(error);
                }
                else{
                    console.log(`[${getTime()}]: row data saved did:${did}, gid:${gid}`);
                    emit2one('phone', 'mapping result', {'did':did, 'gid':gid, 'result':1});    //제스처 저장 성공
                }
            });
        });

        // ir rawdata 보내기 -> ml 이벤트에서 구현
        

        /*
            1. 서버 - ml 이벤트 
        */        
        // ml 학습 결과(성공여부) 받기
        socket.on('get mlResult', (data) => {      
            // 앱에게 학습결과 송신 - 취소
            console.log(`[${getTime()}]: success to update ml model!`)
            emit2one('phone', 'send mlResult', {'result':data['result']});
        });

        // 동작 인식 후 did, gid 받기
        socket.on('get gesture', (data) => {
            //db에서 did & gid로 raw data를 찾는다
            const did = data['did'];
            const gid = data['gid'];

            // DB 서치
            connection.query(`SELECT rawdata, length FROM ipl_gesture.signal WHERE (did, gid) = (${did}, ${gid})`, (error, result, fields) => {
                if (error || result?.[0]?.rawdata == undefined){
                    console.log(error);
                }
                else{
                    const rawData = result[0].rawdata;
                    const length = result[0].length;
                    
                    // 아두이노에게 ir신호 전송
                    emit2one('ir', 'send rawData', {'length':length, 'rawData':rawData});
                    console.log(`[${getTime()}]: send rawdata to ir`);
                }                
            });
        });

        // 학습 모델 보내기 -> 앱 이벤트에서 구현


        /* 
            2. 서버 - 앱 이벤트 
        */
        // 학습 데이터 수신(gid와 사진만)
        socket.on('add gesture', (data) => {
            console.log(`[${getTime()}]: video(current/total): ${data['count']} / ${data['all']}`);

            emit2one('ml', 'add gesture', data);            

        });

        // did & gid 수신 후 아두이노에게 rowData 요청
        socket.on('gesture mapping', (data) => {            
            console.log(`[${getTime()}]: request rawdata to arduino about ${JSON.stringify(data)}`);
            const did = data['did'];
            const gid = data['gid'];
            emit2one('ir', 'request rawData', {'did':did, 'gid':gid, 'message':'send rawdata plz~'});
        });

        // 매핑 데이터 삭제
        socket.on('delete mapping', (data) => {
            console.log(`[${getTime()}]: ${JSON.stringify(data)}`);
            const did = data['did'];
            const gid = data['gid'];

            //디비 저장
            connection.query(`DELETE FROM ipl_gesture.signal WHERE (did, gid) = (${did}, ${gid})`, (error, results, fields) => {
                if (error) {
                    (error.errno == 1062) 
                        ? emit2one('phone', 'delete result', {'did':did, 'gid':gid, 'result':2, 'msg':'None'})    // 없는 제스처
                        : emit2one('phone', 'delete result', {'did':did, 'gid':gid, 'result':0});    // 제스처 저장 실패
                    console.log(error);
                }
                else
                    emit2one('phone', 'delete result', {'did':did, 'gid':gid, 'result':1});    //제스처 저장 성공
            });
        });

        socket.on('my message', (data) => {
            console.log(JSON.stringify(data));
        });
    });
};

