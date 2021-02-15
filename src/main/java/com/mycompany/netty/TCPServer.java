/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetSocketAddress;

/**
 *
 * @author Abustos
 */
public class TCPServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        //NioEventLoopGroup se le puede pasar el numero de hilos que se quieren ejecutar
        //Netty si no se le pasa num thrads  El valor predeterminado aquí es dos veces el número de núcleos de la CPU subprocesos que procesadores / núcleos
        //bossGroup al parecer siempre solo es 1 trhead 
        //https://programmer.group/netty-source-analysis-nioeventloop-group.html
//        https://livebook.manning.com/book/netty-in-action/chapter-8/28
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
           
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_SNDBUF, 8192)
                    .option(ChannelOption.SO_RCVBUF, 8192)
                    .childOption(ChannelOption.SO_SNDBUF, 8192)
                    .childOption(ChannelOption.SO_RCVBUF, 8192)
                    .localAddress(new InetSocketAddress("localhost", 8093))
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //Iniciar los sockets de todas las peticiones entrantes
                    //InitChannel se llama siempre que llaga una nueva conexion tcp
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                        //aca podemos agregar las clases handlers necesarioas para procesar la info
                        ch.pipeline().addLast(new HandlerServer());
                        }
                    });

            // Bind and start to accept incoming connections.
//            b.bind(PORT).sync().channel().closeFuture().sync();
            ChannelFuture channelFuture = b.bind().sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
