<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <div class="section-head">
        <strong>订单中心</strong>
        <span>适合接入订单流转和售后流程</span>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>待支付</span>
            <strong>12</strong>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>待发货</span>
            <strong>38</strong>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="metric-block">
            <span>退款中</span>
            <strong>4</strong>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

