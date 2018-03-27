import React from "react";
import { Flex } from "rebass";
import Icon from "metabase/components/Icon";

const Toast = ({ message }) => (
  <Flex align="center">
    {message}
    <Icon name="close" />
  </Flex>
);

export default Toast;
