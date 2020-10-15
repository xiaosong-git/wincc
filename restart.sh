#jfinal的自启动
#!/bin/sh
file_name="/home/work/restart.log"  #重启脚本的日志，保证可写入，保险一点执行 chmod 777 restart.log
pid=0
proc_num()
{
    num=`ps -ef | grep java | grep -v grep | wc -l`  #此处'sh /home/work/run.sh'替代为实际的，尽量准确，避免误kill
    return $num
}
proc_id()
{
    pid=`ps -ef | grep java | grep -v grep | awk '{print $2}'`  #此处'sh /home/work/run.sh'也替代为实际的
}
proc_num  #执行proc_num()，获取进程数
number=$?  #获取上一函数返回值
if [ $number -eq 0 ]  #如果没有该进程，则重启
then
    /usr/tomcat/wincc/jfinal.sh start  #启动程序的命令
    proc_id
    echo "开始重启",${pid}, `date` >> $file_name  #把重启的进程号、时间 写入日志
fi


#while [ 1 ]
#do
#	j=$(ps -ef |grep java |grep -v "grep"|wc -l)
#	echo "正在监听....."
#	echo $j
#	if [ $j != 1 ]
#	then
#		/usr/tomcat/wincc/jfinal.sh start
#		echo jfinal restart `date +"%Y-%m-%d %H:%M:%S"`>>myErr.log
#	fi
#      sleep 30
#done
