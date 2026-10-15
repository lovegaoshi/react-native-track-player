// @ts-expect-error typing
globalThis.testFunction = async (data: string) => {
  console.log(`[testFunction] input:${data}`);
  return { data: data };
};

export default () => {};
