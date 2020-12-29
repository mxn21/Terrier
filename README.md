# Terrier
IDEA Plugin for Android: show activity stack ; send text to EditText.


  IntelliJ IDEA 与 IntelliJ Platform

IntelliJ IDEA 简称 IDEA，是 Jetbrains 公司旗下的一款 JAVA 开发工具，支持 Java、Scala、Groovy 等语言的开发，同时具备支持目前主流的技术和框架，擅长于企业应用、移动应用和 Web 应用的开发，提供了丰富的功能，智能代码助手、代码自动提示、重构、J2EE支持、各类版本工具(git、svn等)、JUnit、CVS整合、代码分析、 创新的GUI设计等。

IntelliJ Platform 是一个构建 IDE 的开源平台，基于它构建的 IDE 有 IntelliJ IDEA、WebStorm、DataGrip、以及 Android Studio 等等。IDEA 插件也是基于 IntelliJ Platform 开发的。

开发环境搭建

一、开发工具

开发工具使用 Intellij IDEA，下载地址：https://www.jetbrains.com/idea/

IDEA 分为两个版本：

社区版（Community）：完全免费，代码开源，但是缺少一些旗舰版中的高级特性
旗舰版（Ultimate）：30天免费，支持全部功能，代码不开源

创建一个插件工程

选择 File | New | Project，左侧栏中选择 IntelliJ Platform Plugin 工程类型
resources/META-INF/plugin.xml 插件的配置文件，指定插件名称、描述、版本号、支持的 IntelliJ IDEA 版本、插件的 components 和 actions 以及软件商等信息
plugin.xml文件说明
<idea-plugin>
    
  <!-- 插件唯一id，不能和其他插件项目重复，所以推荐使用com.xxx.xxx的格式
       插件不同版本之间不能更改，若没有指定，则与插件名称相同 -->
  <id>com.your.company.unique.plugin.id</id>
   
  <!-- 插件名称，别人在官方插件库搜索你的插件时使用的名称 -->
  <name>Plugin display name here</name>
  
  <!-- 插件版本号 -->
  <version>1.0</version>
    
  <!-- 供应商主页和email（不能使用默认值，必须修改成自己的）-->
  <vendor email="support@yourcompany.com" url="http://www.yourcompany.com">YourCompany</vendor>
  <!-- 插件的描述 （不能使用默认值，必须修改成自己的。并且需要大于40个字符）-->
  <description><![CDATA[
      Enter short description for your plugin here.<br>
      <em>most HTML tags may be used</em>
    ]]></description>
  <!-- 插件版本变更信息，支持HTML标签；
       将展示在 settings | Plugins 对话框和插件仓库的Web页面 -->
  <change-notes><![CDATA[
      Add change notes here.<br>
      <em>most HTML tags may be used</em>
    ]]>
  </change-notes>

 <!-- 插件兼容IDEAbuild 号-->
  <idea-version since-build="173.0"/>

  <!-- 插件所依赖的其他插件的id -->
  <depends>com.intellij.modules.platform</depends>

  <extensions defaultExtensionNs="com.intellij">
  <!-- 声明该插件对IDEA core或其他插件的扩展 -->
  </extensions>

  <!-- 编写插件动作 -->
  <actions>
  </actions>

</idea-plugin>

组件

ToolWindow

ToolWindow 就是依附在左右两侧或底部的窗口, 可以最小化成一个按钮, 或展开, 改变大小和位置关闭. 在菜单栏中 View => Tool Window 列表中可以看到当前所有的 ToolWindow.
在 plugin.xml 中注册, ToolWindow 需要放在 extensions 标签中.
<extensions defaultExtensionNs="com.intellij">
    <toolWindow id="TestToolWindow"
                canCloseContents="false"
                factoryClass="com.your_domain.TestToolWindow"
                anchor="bottom"/>
</extensions>

其中, id 是 ToolWindow 的标题, canCloseContents 设置是否可以关闭, factoryClass 就是实现了 ToolWindowFactory 的该 ToolWindow 的工厂类. anchor 为显示位置

什么是 PSI

PSI 是 Program Structure Interface 的缩写, 
每一种语言都有对应的 PsiFile 接口
Kotlin 类对应的 PSI 接口是 KtClass, 文件对应的是 KtFile
Java 类对应的 PSI 接口是 PsiClass, 文件对应的是 PsiJavaFile
一个源码文件的所有的元素都是 PsiElement 的子类, 包括 PsiFile, 比如在 Java 源码文件 PsiJavaFile 中 , 关键词 private, public 对应的 PsiElement 是 PsiKeyword. 通过PsiElement#acceptChild 方法可以遍历一个 element的所有子元素. 通过 PsiElement 的 add, delete, replace 等方法, 可以轻松的操作 PsiElement
创建一个用于 Java 的 PsiElement


什么是 VFS

VFS 是 Virtual File System 的缩写, 它封装了大部分对活动文件的操作, 它提供了一个处理文件通用 API, 可以追踪文件变化
