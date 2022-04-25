Qmsg.config({
    timeout:3000
});
function load(){
    let path = window.location.pathname;
    //bind upload
    $('#upload').change(()=>{
        var fileItem = $('#upload')[0].files[0];
        var formData = new FormData();
            formData.append('file', fileItem);
        $.ajax({
            url: path,
            type: "post",
            data: formData,
            processData: false,
              contentType: false,
            success: (msg)=>{
                //alert(msg);
                Qmsg.info(msg);
                listFile(path);
            }
        }).fail((res)=>Qmsg.error(res.responseText));
    });
    //bind mkdir
    $('#mkdir').on('click', ()=>{
        var newdir = prompt("新建文件夹");
        if(null==newdir||""==newdir)return;
        $.ajax({
            url: path,
            type: "put",
            data: newdir,
            success: (msg)=>{
                Qmsg.info(msg);
                listFile(path);
            }
        }).fail((res)=>Qmsg.error(res.responseText));
    });
    //bind delete
    $('.delete').live('click', (point)=>{
    var deletepath = $(point.target).attr("data");
    if(confirm("确认删除文件(夹)"+deletepath))
        $.ajax({
            url: path,
            type: "delete",
            data: deletepath,
            success: (msg)=>{
                Qmsg.info(msg);
                listFile(path);
            }
        }).fail((res)=>Qmsg.error(res.responseText));
    });
    listFile(path);
}
function listFile(path){
    $.ajax({
        url:path+"?action=list",
        type: "get",
        success: (jsonArray)=>{
            $("#list").empty();
            if("/" != path){
                $("#list").append($("<tr><td><a href='..'>..</a></td><td>---</td><td>---</td></tr>"));
            }
            if(0==jsonArray.length){
                $("#list").append($("<tr><td class='center' colspan=3>Empty Directory</td></tr>"));
            }else{
                $.each(jsonArray,function(index,json){
                    var name= json.name;
                    var href = path + (path.endsWith('/')?'':'/') + name;
                    var size = json.isFile?humanSize(json.size):"---";
                    var lastModified = new Date(json.lastModified);
                    lastModified = lastModified.getFullYear()+"-"+fix2(lastModified.getMonth()+1)+"-"+fix2(lastModified.getDate())+" "+fix2(lastModified.getHours())+":"+fix2(lastModified.getMinutes())+":"+fix2(lastModified.getSeconds());
                    var tr = $(`<tr><td><a href='${ href }'>${ name }</a></td><td class='center'>${ size }</td><td class='center'>${ lastModified }</td><td><button class="delete" data='${ name }'>delete</button></td></tr>`);
                    $("#list").append(tr);
                })
            }
        }
    }).fail((res)=>Qmsg.error(res.responseText));
}
function fix2(num){
	if(num<10)return '0'+num;
	return num;
}
function humanSize(size){
    if (size > 1024 * 1024 * 1024) {
        return (size / (1024 * 1024 * 1024)).toFixed(2) + "G";
    }
    if (size > 1024 * 1024) {
        return (size / (1024 * 1024)).toFixed(2) + "M";
    }
    if (size > 1024 ) {
        return (size / (1024)).toFixed(2) + "K";
    }
    return size + "B";
}