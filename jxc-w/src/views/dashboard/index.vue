<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
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

