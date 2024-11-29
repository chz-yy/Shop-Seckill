//  通用的ajax方法
function ajaxHttp (obj, callback, err) {
    // 添加调试日志
    console.log('发起请求:', obj);
    
    // 构建完整URL
    const baseUrl = 'http://localhost:9010';
    const fullUrl = baseUrl + obj.url;
    console.log('完整请求URL:', fullUrl);

    $.ajax(fullUrl, {
        type: obj.type || 'get',
        contentType: obj.contentType || 'application/json;charset=UTF-8',
        headers:{'token': localStorage.getItem('token')},
        data: obj.data || {},
        success: function (res) {
            console.log('请求响应:', res);  // 添加响应日志
            if (res.code === 200) {
                callback(res)
            } else {
                if (res.code === -2 || res.code === -3) {
                    localStorage.removeItem('token')
                    $('.commodity-header').find('.seckill-shopping').css('display', 'block')
                    setTimeout(function () {
                        layer.confirm('请先进行登录！！', {
                            btn: ['马上登录','取消'] //按钮
                        }, function(){
                            window.location.href = '/login.html'
                        }, function(index){
                            layer.close(index)
                        });
                    }, 200)
                }else{
                    layer.msg(res.msg)
                }
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.error('请求失败:', textStatus, errorThrown);
            if (err) {
                err(jqXHR, textStatus, errorThrown);
            } else {
                defaultError();
            }
        }
    })
}
function defaultError(){
    layer.msg('网站繁忙，稍后再试！')
}