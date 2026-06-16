<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<script>
export default {
  name: 'App'
};
</script>

<style>
* {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
  color: #14231d;
}
</style>
