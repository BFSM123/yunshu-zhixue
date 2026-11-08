# MyBatis XML

- 简单 CRUD 使用 MyBatis-Plus 的 `BaseMapper`，不写 XML；
- 复杂统计查询（看板趋势、掌握率、正确率等）在本目录按模块建 XML，命名 `XxxMapper.xml`；
- XML 只能查询本模块拥有的表，跨模块取数走对方 Service 接口。
