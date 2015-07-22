var Chat = {};
Chat.socket = null;
var gameRoomID;
var playerList;
var flag=true;
var roleSelected;
var colorArray;
var mode;
var dead_count;

var timeInterval,scoreInterval,createInterval,moveInterval;
var isDead = [0,0,0,0];
var playerScore,result;

var game1;
var game1Width;
var gameMaxRight;
var basketWidth;

$(document).ready(function(){
    mod = 0; //initial mod
    playerList = new Array();
    roleSelected = [0,0,0,0];
    colorArray = ['#FF0000','#FF6600','#33CC33','#3399FF'];
    
    $("#mask_down_p").animate({'font-size': '8em'}, 1000,function(){});
    //Start game
	$("#welcome_btn").click(function(event) {
		/* Act on the event */
        Chat.initialize();
	});

    //override here
    $('#submit').click(function(){
        if(playerList.length <= 0){
            alert("Please connect your cellphone!");
        }else{
            //start Game
            $("#welcome-page").fadeOut('fast', function() {
                $("#game-page").fadeIn('fast', function() {
                    startGame();
                    mod = 2;
                });
            });
            
        }
    });

    //
    $("#top_logo").click(function(event) {
        /* Act on the event */
        $("#top_logo_p").html(" ");
        $("#mask_up").slideUp(500);
        $("#mask_down").animate({"height": "0px"}, 1000,function(){
            $("#mask").css('display', 'none');
            $("#top_logo_p").delay(500).html("Flipit");
        });
        $("#top_logo").animate({"top": "0px"}, 500);
        $("#logo").fadeIn('fast');
    });

    $("#logo").hover(function() {
        /* Stuff to do when the mouse enters the element */
        $("#top_logo").addClass('logo_hover');
    }, function() {
        /* Stuff to do when the mouse leaves the element */
        $("#top_logo").removeClass('logo_hover');
    });
    
});

Chat.connect = (function(host) {
    if ('WebSocket' in window) {
        Chat.socket = new WebSocket(host);
    } else if ('MozWebSocket' in window) {
        Chat.socket = new MozWebSocket(host);
    } else {
        console.log('Error: WebSocket is not supported by this browser.');
        return;
    }

    Chat.socket.onopen = function () {
        console.log('Info: WebSocket connection opened.');
        $('#box').css('display', 'none');
        $('#pair_box').fadeIn('fast',function(){});
    };

    Chat.socket.onclose = function () {
        alert('Error:Server closed!');
    };

    Chat.socket.onmessage = function (message) {
        if(mod == 2){
            var strArray = message.data.toString().split(",");
            playerID = parseInt(strArray[0]);
            playerMotion = strArray[1].toString();
            motionResponse(playerID, playerMotion);
            console.log("[Debug]Player"+playerID+":"+playerMotion);

        }else if(mod == 1){
            var strArray = message.data.toString().split(",");
            playerID = parseInt(strArray[0]);
            playerMotion = strArray[1].toString();
            console.log("[Debug]Player"+playerID+":"+playerMotion);

            var player = getPlayer(playerID);
            if(player == null && playerList.length < 4){
                //New player 
                
                player = new Player(playerID);
                playerList.push(player);
                //animate
                player_enter("#player" + playerList.length, player.playerColor);
                //Game 
                $('#game1').append("<div class='basket' id='"+player.playerID+"'></div>");
                $('#score').append("<div class='score' id='score_"+ player.playerID+"'>0<div>");
                $("#arrow3").css('background-image', 'url(./img/arrow3_f.png)');
                $('#' + player.playerID).css({'background-image':'url(./img/jet'+(player.playerID)+'.png)'});
                $('#score_' + player.playerID).css({'background':colorArray[player.playerID-1]});
                console.log("[Debug]New Player"+(player.playerID));
            }
            console.log('[Debug]l:'+playerList.length);
            //motionResponse(player, playerMotion);
        }else if(mod == 0){
            var strArray = message.data.toString().split(",");
            if(strArray[0] != "port" && strArray.length() < 2){
                console.log("Error:"+message.data);
                alert("Server Error!");
                gameRoomID = "error";
            }else {
                gameRoomID = strArray[1];
            }
            $('#pair_id').val(gameRoomID);
            mod = 1;
        }
    };
});

Chat.initialize = function() {
    if (window.location.protocol == 'http:') {
        Chat.connect('ws://140.114.79.67:8080/flipit-Server/websocket/chat');
    } else {
        Chat.connect('ws://140.114.79.67:8080/flipit-Server/websocket/chat');
    }
};

function player_enter(newPlayer, newPlayerColor){
    $(newPlayer).animate({'height': '100%'}, 'fast');
    $(newPlayer).css('background-image', 'url(./img/phone_focus.png)');
    $(newPlayer).animate({'margin-top': '-20px'}, 100,function(){
        $(this).animate({'margin-top': '-10px'}, 100, function() {
            $(this).animate({'margin-top': '-20px'}, 100, function() {
                $(this).animate({'margin-top': '-10px'}, 100, function() {
                    $(this).animate({'margin-top': '-20px'}, 100, function() {
                        $(this).animate({'margin-top': '-10px'}, 100);
                        $(this).css({"border-bottom":"4px solid " + newPlayerColor});
                    });
                });
            });
        });
    });
}

function Player(playerID){
  this.playerID = playerID;
  this.roleIdx = playerID - 1;
  this.playerColor = colorArray[playerID-1];
}

function getPlayer(playerID){
    for (idx in playerList){
      if(playerID == playerList[idx].playerID) return playerList[idx];
    }
    return null;
}

function motionResponse(playerID, playerMotion){
    var position = $('#'+playerID).position();
    //console.log("position.left:"+ position.left +",basketWidth:" + basketWidth);
    //console.log("game1.left:" + game1.left + ",game1Width:" + game1Width);

    switch(playerMotion[0]){
      case 'r':
        if( (position.left+basketWidth+30) < (gameMaxRight) ){
            $('#'+playerID).animate({"left":"+=30"},5); 
        }
        break;
      case 'l':
        if( (position.left-30) > (game1.left) ){
            $('#'+playerID).animate({"left":"-=30"},5);
        }
        break;
      case 'u':
        break;
      case 'd':
        break;
      case 'o':
        break;
      case 'leave':
        break
      default:
        console.log('[Debug]playerMotion:'+playerMotion);
        break;
    }
}

function startGame(){
    dead_count = 0;
    $('#basket').css({"left":$('#game1').position().left + 10});
    game1 = $('#game1').position();
    game1Width = $('#game1').width();
    gameMaxRight = game1.left+game1Width;
    basketWidth = $('.basket').width();
    Chat.socket.send("go");

    createInterval = setInterval(createEgg, 500);
    moveInterval = setInterval(moveEgg, 100);
    timeInterval = setInterval(countTime,1000);
    scoreInterval = setInterval(countScore,100);
}
var egg_html1 = "<div class='egg' style='left:";
var egg_html2 = "px;'></div>";

function createEgg(){
    var posX = ( $('#game1').position().left + Math.random()*($('#game1').width() - 50) ).toString();
    $('#game1').append(egg_html1.concat(posX).concat(egg_html2));
}

function moveEgg(){
    var eggY, eggX;
    var basketX, basketY;
    $('.egg').each(function(){
        eggY = $(this).position().top;
        eggX = $(this).position().left;
        $(this).animate({'top':'+=30'},30);

        $('.basket').each(function(idx){
            basketX = $(this).position().left;
            basketY = $(this).position().top;
            //console.log('basketX:'+basketX+",basketY:"+basketY);
            if(checkInBasket(eggX, eggY, basketX, basketY)){
                $(this).remove();
                dead_count++;
                isDead[idx] = 1;
                Chat.socket.send(idx+1+",dead");
                if(dead_count >= playerList.length){
                    clearTimeout(timeInterval);
                    clearTimeout(scoreInterval);
                    clearTimeout(createInterval);
                    clearTimeout(moveInterval);
                    scoreSort();
                    mod=-1;
                    //gameRoom.child(gameRoomID).set('end');
                }
            }
        });

        if(eggY >= $(window).height())$(this).remove();
    });

}
function isAllDead(){
    //if()
    return false;
}
function countTime(){
    $('#time').html($('#time').html()-1);
    
    if($('#time').html() == '0'){

        $('.basket').each(function(idx){
            if(!isDead[idx])Chat.socket.send(idx+1+",dead");
        });

        clearTimeout(timeInterval);
        clearTimeout(scoreInterval);
        clearTimeout(createInterval);
        clearTimeout(moveInterval);
        scoreSort();
        //gameRoom.child(gameRoomID).set('end');
        
    }
}

function countScore(){
    $('.score').each(function(idx){
        if(!isDead[idx]){
            $(this).html(parseInt($(this).html())+10);
        }
    });
}

function checkInBasket(eggX, eggY, basketX, basketY){
    //console.log("eggX:" + eggX + ",eggY:" + eggY +",basketX:"+basketX+",basketY:"+basketY);
    var eggWidth = $('.egg').width();
    var basketWidth = $('.basket').width();
    if(eggX >= basketX && (eggX+eggWidth <= basketX + basketWidth) && (eggY+eggWidth > basketY)){
        return true;
    }
    else 
        return false;
}

function scoreSort(){
    playerScore = new Array();
    $('.score').each(function(idx){
        var player = new Object;
        player.roleImg = "jet"+playerList[idx].playerID+".png"; // corresponding color
        player.score = parseInt($(this).html());
        playerScore[idx] = player;
    });
    result = playerScore.sort(cmpScore).reverse();
    //sendscore(result);
    showResult();
    //alert(result[0].roleColor);
}

function sendscore(result){
   $.ajax({ 
          data:{result:result},
          type:'post', 
          url:'game.php', 
          success:function(res) 
          { 
           //  alert('success');
            } 
        }); 
}

function showResult(){
    for (idx in result){
        $('#resultTable').append("<tr><td colspan='5'><img width='100px' height='100px' src='img/"+result[idx].roleImg+"'></td><td>"+result[idx].score+"</td><tr>");
    }
    $('#game-result-wrapper').fadeIn('fast');
}
function cmpScore(a,b){
    return a.score - b.score;
}

