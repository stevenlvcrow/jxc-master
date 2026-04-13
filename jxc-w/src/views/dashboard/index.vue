<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <div class="section-head">
        <strong>仪表盘</strong>
        <span>已接入统一页面模板，可按模块继续扩展数据看板</span>
      </div>
      <el-row :gutter="8">
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>今日销售单</span>
            <strong>0</strong>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>待处理采购</span>
            <strong>0</strong>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>库存预警</span>
            <strong>0</strong>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

